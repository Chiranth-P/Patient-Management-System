package com.pm.stack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerDefinitionOptions;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.FargateService;
import software.amazon.awscdk.services.ecs.FargateTaskDefinition;
import software.amazon.awscdk.services.ecs.LogDriver;
import software.amazon.awscdk.services.ecs.PortMapping;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.rds.Credentials;
import software.amazon.awscdk.services.rds.DatabaseInstance;
import software.amazon.awscdk.services.rds.DatabaseInstanceEngine;
import software.amazon.awscdk.services.rds.PostgresEngineVersion;
import software.amazon.awscdk.services.rds.PostgresInstanceEngineProps;

public class AwsStack extends Stack {
    public AwsStack(final App scope, final String id, final StackProps props) {
        super(scope, id, props);

        Vpc vpc = Vpc.Builder.create(this, "PatientManagementVPC").maxAzs(2).build();
        Cluster ecsCluster = Cluster.Builder.create(this, "PatientManagementCluster").vpc(vpc).build();

        DatabaseInstance authServiceDb = createDatabase("AuthServiceDB", "authservicedb", vpc);
        DatabaseInstance patientServiceDb = createDatabase("PatientServiceDB", "patientservicedb", vpc);

        FargateService authService = createFargateService(this, "AuthService", ecsCluster, "auth-service", List.of(4005),
                Map.of("JWT_SECRET", "Y2hhVEc3aHJnb0hYTzMyZ2ZqVkpiZ1RkZG93YWxrUkM="), authServiceDb, "postgres");

        createFargateService(this, "BillingService", ecsCluster, "billing-service", List.of(4001, 9001), null, null, null);
        createFargateService(this, "AnalyticsService", ecsCluster, "analytics-service", List.of(4002), Map.of("SPRING_KAFKA_BOOTSTRAP_SERVERS", "kafka:29092"), null, null);

        FargateService patientService = createFargateService(this, "PatientService", ecsCluster, "patient-service", List.of(4000),
                Map.of("BILLING_SERVICE_ADDRESS", "billing-service.local", "BILLING_SERVICE_GRPC_PORT", "9001", "SPRING_KAFKA_BOOTSTRAP_SERVERS", "kafka:29092"),
                patientServiceDb, "mysql");

        ApplicationLoadBalancedFargateService.Builder.create(this, "ApiGatewayService")
                .cluster(ecsCluster)
                .cpu(256).memoryLimitMiB(512)
                .desiredCount(1)
                .taskImageOptions(ApplicationLoadBalancedTaskImageOptions.builder()
                        .image(ContainerImage.fromEcrRepository(Repository.fromRepositoryName(this, "api-gatewayRepo", "api-gateway")))
                        .environment(Map.of("AUTH_SERVICE_URL", "http://auth-service.local:4005"))
                        .build())
                .publicLoadBalancer(true)
                .build();
    }

    private DatabaseInstance createDatabase(String id, String dbName, Vpc vpc) {
        return DatabaseInstance.Builder
                .create(this, id)
                .engine(DatabaseInstanceEngine.postgres(PostgresInstanceEngineProps.builder().version(PostgresEngineVersion.VER_15).build()))
                .vpc(vpc)
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE3, InstanceSize.MICRO))
                .credentials(Credentials.fromGeneratedSecret(dbName + "user"))
                .databaseName(dbName)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
    }

    private FargateService createFargateService(Stack stack, String id, Cluster ecsCluster, String imageName, List<Integer> ports,
                                                Map<String, String> additionalEnvVars, DatabaseInstance db, String dbType) {
        FargateTaskDefinition taskDefinition = FargateTaskDefinition.Builder.create(stack, id + "Task")
                .cpu(256).memoryLimitMiB(512).build();

        Map<String, String> envVars = new HashMap<>(additionalEnvVars != null ? additionalEnvVars : Map.of());
        if (db != null) {
            String jdbcUrl = dbType.equals("postgres")
                    ? String.format("jdbc:postgresql://%s:%s/%s", db.getDbInstanceEndpointAddress(), db.getDbInstanceEndpointPort(), db.getDbInstanceEndpointAddress())
                    : String.format("jdbc:mysql://%s:%s/%s", db.getDbInstanceEndpointAddress(), db.getDbInstanceEndpointPort(), db.getDbInstanceEndpointAddress());
            envVars.put("SPRING_DATASOURCE_URL", jdbcUrl);
            envVars.put("SPRING_DATASOURCE_USERNAME", db.getSecret().secretValueFromJson("username").toString());
            envVars.put("SPRING_DATASOURCE_PASSWORD", db.getSecret().secretValueFromJson("password").toString());
            envVars.put("SPRING_JPA_HIBERNATE_DDL_AUTO", "update");
            envVars.put("SPRING_SQL_INIT_MODE", "always");
        }

        ContainerImage image = ContainerImage.fromEcrRepository(Repository.fromRepositoryName(stack, imageName + "Repo", imageName));
        ContainerDefinitionOptions.Builder containerOptions = ContainerDefinitionOptions.builder()
                .image(image)
                .portMappings(ports.stream().map(port -> PortMapping.builder().containerPort(port).build()).collect(Collectors.toList()))
                .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                        .logGroup(LogGroup.Builder.create(stack, id + "LogGroup").logGroupName("/ecs/" + imageName).removalPolicy(RemovalPolicy.DESTROY).build())
                        .streamPrefix(imageName).build()))
                .environment(envVars);

        taskDefinition.addContainer(imageName + "Container", containerOptions.build());

        return FargateService.Builder.create(stack, id + "Service")
                .cluster(ecsCluster).taskDefinition(taskDefinition).serviceName(imageName).build();
    }
}