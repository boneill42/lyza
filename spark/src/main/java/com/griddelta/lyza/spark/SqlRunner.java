package com.griddelta.lyza.spark;

import static com.datastax.spark.connector.japi.CassandraJavaUtil.javaFunctions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.rdd.RDD;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scala.collection.JavaConversions;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ColumnMetadata;
import com.datastax.driver.core.KeyspaceMetadata;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.TableMetadata;
import com.datastax.spark.connector.japi.CassandraRow;

public class SqlRunner implements Serializable {
    private static final long serialVersionUID = 1L;
    private static Logger LOGGER = LoggerFactory.getLogger(SqlRunner.class);

    private transient JavaSparkContext context;
    private transient SQLContext sqlContext;
    private transient Metadata metadata;

    @SuppressWarnings("serial")
    public SqlRunner(String sparkMasterUrl, String cassandraHost, int cassandraPort, String keyspace, String table) {
        SparkConf conf = new SparkConf();
        conf.setAppName("Java API demo");
        conf.setMaster(sparkMasterUrl);
        conf.set("spark.cassandra.connection.host", cassandraHost);
        context = new JavaSparkContext(conf);
        sqlContext = new SQLContext(context);
        Cluster cluster = Cluster.builder().addContactPoints(cassandraHost).withPort(cassandraPort).build();
        metadata = cluster.getMetadata();

        RDD<Row> cassandraRDD = javaFunctions(context).cassandraTable("test_keyspace", "products")
                .map(new Function<CassandraRow, Row>() {
                    public Row call(CassandraRow cassandraRow) throws Exception {
                        List<Object> values = JavaConversions.asJavaList(cassandraRow.columnValues());
                        return RowFactory.create(values.toArray());
                    }
                }).rdd();
        cassandraRDD.cache();
        LOGGER.info("Total Records Cached = [" + cassandraRDD.count() + "]");

        StructType schema = getSchema(keyspace, table);
        DataFrame dataFrame = sqlContext.createDataFrame(cassandraRDD, schema);
        sqlContext.registerDataFrameAsTable(dataFrame, table);
    }

    private StructType getSchema(String keyspace, String table) {
        KeyspaceMetadata keyspaceMetadata = metadata.getKeyspace(keyspace);
        TableMetadata tableMetadata = keyspaceMetadata.getTable(table);
        List<StructField> fields = new ArrayList<StructField>();
        for (ColumnMetadata columnMetadata : tableMetadata.getColumns()) {
            com.datastax.driver.core.DataType dataType = columnMetadata.getType();
            if (dataType == com.datastax.driver.core.DataType.ascii()) {
                fields.add(DataTypes.createStructField(columnMetadata.getName(), DataTypes.StringType, true));
            } else if (dataType == com.datastax.driver.core.DataType.cint()) {
                fields.add(DataTypes.createStructField(columnMetadata.getName(), DataTypes.IntegerType, true));
            } else if (dataType == com.datastax.driver.core.DataType.cboolean()) {
                fields.add(DataTypes.createStructField(columnMetadata.getName(), DataTypes.BooleanType, true));
            } else if (dataType == com.datastax.driver.core.DataType.cfloat()) {
                fields.add(DataTypes.createStructField(columnMetadata.getName(), DataTypes.FloatType, true));
            } else if (dataType == com.datastax.driver.core.DataType.bigint()) {
                fields.add(DataTypes.createStructField(columnMetadata.getName(), DataTypes.LongType, true));
            } else if (dataType == com.datastax.driver.core.DataType.cdouble()) {
                fields.add(DataTypes.createStructField(columnMetadata.getName(), DataTypes.DoubleType, true));
            } else if (dataType == com.datastax.driver.core.DataType.timestamp()) {
                fields.add(DataTypes.createStructField(columnMetadata.getName(), DataTypes.TimestampType, true));
            } else if (dataType == com.datastax.driver.core.DataType.varchar()) {
                fields.add(DataTypes.createStructField(columnMetadata.getName(), DataTypes.StringType, true));
            } else if (dataType == com.datastax.driver.core.DataType.text()) {
                fields.add(DataTypes.createStructField(columnMetadata.getName(), DataTypes.StringType, true));
            } else {
                throw new NotImplementedException("Sorry [" + dataType + "] type is not supported yet.");
            }
            // TODO: Map remaining types.
        }

        StructType schema = DataTypes.createStructType(fields);
        LOGGER.debug("Schema = [{}]", schema);
        return schema;

    }

    public DataFrame query(String sql) {
        DataFrame result = null;
        try {
            long start = System.currentTimeMillis();
            LOGGER.info("Running query: [" + sql + "]");
            result = sqlContext.sql(sql);
            long count = result.count();
            long stop = System.currentTimeMillis();
            LOGGER.info("DONE: [" + count + "] records returned in [" + (stop - start) + "] milliseconds.");
        } catch (Exception e) {
            LOGGER.error("ERROR", e);
        }
        return result;
    }

    public static void main(String[] args) {
        LOGGER.debug("Executing sqlRunner with [" + args + "]");
        if (args.length != 5) {
            System.err
                    .println("Syntax: com.griddelta.lyza.SqlRunner <sparkMasterUrl> <cassandraHost> <cassandraPort> <keyspace> <table>");
            System.exit(1);
        }
        SqlRunner sqlRunner = new SqlRunner(args[0], args[1], Integer.parseInt(args[2]), args[3], args[4]);
        sqlRunner.query("SELECT id, price from products WHERE price < 0.50");
    }
}
