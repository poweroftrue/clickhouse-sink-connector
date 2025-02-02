from integration.requirements.requirements import *
from integration.tests.steps.configurations import *
from integration.tests.steps.sql import *
from integration.tests.steps.datatypes import *


@TestOutline
def mysql_to_clickhouse_inserts(
    self, input, output, mysql_columns, clickhouse_table, clickhouse_columns=None
):
    """`INSERT` check section"""

    table_name = f"insert_{getuid()}"
    mysql = self.context.cluster.node("mysql-master")

    init_sink_connector(
        auto_create_tables=clickhouse_table[0], topics=f"SERVER5432.test.{table_name}"
    )

    with Given(f"I create MySql to CH replicated table", description=table_name):
        create_mysql_to_clickhouse_replicated_table(
            name=table_name,
            mysql_columns=mysql_columns,
            clickhouse_table=clickhouse_table,
            clickhouse_columns=clickhouse_columns,
        )

    with When("I insert data in MySql table"):
        mysql.query(f"INSERT INTO {table_name} (col1,col2,col3) VALUES {input};")

    with Then("I check data inserted correct"):
        complex_check_creation_and_select(
            table_name=table_name,
            manual_output=output,
            clickhouse_table=clickhouse_table,
            statement="col1,col2,col3",
            with_final=True,
        )


@TestScenario
def null_default_insert(
    self,
    input="(DEFAULT,5,DEFAULT)",
    output="\\N,5,777",
    mysql_columns="col1 INT, col2 INT NOT NULL, col3 INT default 777",
    clickhouse_columns="col1 Nullable(Int32), col2 Int32, col3 Int32",
):
    """NULL and DEFAULT `INSERT` check."""
    for clickhouse_table in available_clickhouse_tables:
        with Example({clickhouse_table}, flags=TE):
            mysql_to_clickhouse_inserts(
                input=input,
                output=output,
                mysql_columns=mysql_columns,
                clickhouse_columns=clickhouse_columns,
                clickhouse_table=clickhouse_table,
            )


@TestScenario
def null_default_insert_2(
    self,
    input="(DEFAULT,5,333)",
    output="\\N,5,333",
    mysql_columns="col1 INT, col2 INT NOT NULL, col3 INT default 777",
    clickhouse_columns="col1 Nullable(Int32), col2 Int32, col3 Int32",
):
    """NULL and DEFAULT `INSERT` check."""
    for clickhouse_table in available_clickhouse_tables:
        with Example({clickhouse_table}, flags=TE):
            mysql_to_clickhouse_inserts(
                input=input,
                output=output,
                mysql_columns=mysql_columns,
                clickhouse_columns=clickhouse_columns,
                clickhouse_table=clickhouse_table,
            )


@TestScenario
def select_insert(
    self,
    input="((select 2),7,DEFAULT)",
    output="2,7,777",
    mysql_columns="col1 INT, col2 INT NOT NULL, col3 INT default 777",
    clickhouse_columns="col1 Int32, col2 Int32, col3 Int32",
):
    """SELECT and DEFAULT `INSERT` check."""
    for clickhouse_table in available_clickhouse_tables:
        with Example({clickhouse_table}, flags=TE):
            mysql_to_clickhouse_inserts(
                input=input,
                output=output,
                mysql_columns=mysql_columns,
                clickhouse_columns=clickhouse_columns,
                clickhouse_table=clickhouse_table,
            )


@TestScenario
def select_insert_2(
    self,
    input="((select 2),7,DEFAULT)",
    output="2,7,777",
    mysql_columns="col1 INT, col2 INT NOT NULL, col3 INT default 777",
    clickhouse_columns="col1 Int32, col2 Int32, col3 Int32",
):
    """simple `INSERT` check."""
    for clickhouse_table in available_clickhouse_tables:
        with Example({clickhouse_table}, flags=TE):
            mysql_to_clickhouse_inserts(
                input=input,
                output=output,
                mysql_columns=mysql_columns,
                clickhouse_columns=clickhouse_columns,
                clickhouse_table=clickhouse_table,
            )


@TestFeature
@Requirements(RQ_SRS_030_ClickHouse_MySQLToClickHouseReplication_Queries_Inserts("1.0"))
@Name("insert")
def feature(self):
    """Different `INSERT` tests section."""
    # xfail("")

    with Given("I enable debezium and sink connectors after kafka starts up"):
        init_debezium_connector()

    for scenario in loads(current_module(), Scenario):
        scenario()
