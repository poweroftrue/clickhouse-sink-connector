package com.altinity.clickhouse.debezium.embedded.ddl.parser;

import com.altinity.clickhouse.debezium.embedded.cdc.DebeziumChangeEventCapture;
import com.altinity.clickhouse.debezium.embedded.parser.SourceRecordParserService;
import com.altinity.clickhouse.sink.connector.db.BaseDbWriter;
import org.apache.log4j.BasicConfigurator;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Testcontainers
public class ClickHouseDebeziumEmbeddedDDLAddColumnIT extends ClickHouseDebeziumEmbeddedDDLBaseIT {

    @BeforeEach
    public void startContainers() throws InterruptedException {
        mySqlContainer = new MySQLContainer<>(DockerImageName.parse("docker.io/bitnami/mysql:latest")
                .asCompatibleSubstituteFor("mysql"))
                .withDatabaseName("employees").withUsername("root").withPassword("adminpass")
                .withInitScript("alter_ddl_add_column.sql")
                .withExtraHost("mysql-server", "0.0.0.0")
                .waitingFor(new HttpWaitStrategy().forPort(3306));

        BasicConfigurator.configure();
        mySqlContainer.start();
        Thread.sleep(15000);
    }

    @Test
    public void testAddColumn() throws Exception {

        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.execute(() -> {
            try {
                new DebeziumChangeEventCapture().setup(getDebeziumProperties(), new SourceRecordParserService(),
                        new MySQLDDLParserService());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Thread.sleep(10000);

        Connection conn = connectToMySQL();
        // alter table ship_class change column class_name class_name_new int;
        // alter table ship_class change column tonange tonange_new decimal(10,10);

        conn.prepareStatement("alter table ship_class add column ship_spec varchar(150) first, add somecol int after start_build, algorithm=instant;").execute();
        conn.prepareStatement("alter table ship_class ADD newcol bool null DEFAULT 0;").execute();
        conn.prepareStatement("alter table ship_class add column customer_address varchar(100) not null, add column customer_name varchar(20) null;").execute();
        conn.prepareStatement("alter table add_test add column col8 varchar(255) first;").execute();
        conn.prepareStatement("alter table add_test add column col99 int default 1 after col8;").execute();

        conn.prepareStatement("alter table add_test modify column col99 tinyint;").execute();
        conn.prepareStatement("alter table add_test add column col22 varchar(255);").execute();
        conn.prepareStatement("alter table add_test add column col4 varchar(255);").execute();
        conn.prepareStatement("alter table add_test rename column col99 to col101;").execute();
        conn.prepareStatement(" alter table add_test drop column col101;").execute();

        Thread.sleep(15000);


        BaseDbWriter writer = new BaseDbWriter(clickHouseContainer.getHost(), clickHouseContainer.getFirstMappedPort(),
                "employees", clickHouseContainer.getUsername(), clickHouseContainer.getPassword(), null);

        Map<String, String> shipClassColumns = writer.getColumnsDataTypesForTable("ship_class");
        Map<String, String> addTestColumns = writer.getColumnsDataTypesForTable("add_test");

        // Validate all ship_class columns.
        Assert.assertTrue(shipClassColumns.get("ship_spec").equalsIgnoreCase("String"));
        Assert.assertTrue(shipClassColumns.get("somecol").equalsIgnoreCase("Int32"));
        Assert.assertTrue(shipClassColumns.get("newcol").equalsIgnoreCase("Nullable(Bool)"));
        Assert.assertTrue(shipClassColumns.get("customer_address").equalsIgnoreCase("String"));
        Assert.assertTrue(shipClassColumns.get("customer_name").equalsIgnoreCase("Nullable(String)"));

        // Validate all add_test columns.
        Assert.assertTrue(addTestColumns.get("col8").equalsIgnoreCase("String"));
        Assert.assertTrue(addTestColumns.get("col2").equalsIgnoreCase("Int32"));
        Assert.assertTrue(addTestColumns.get("col3").equalsIgnoreCase("Int32"));

        // Files.deleteIfExists(tmpFilePath);
        executorService.shutdown();

    }
}