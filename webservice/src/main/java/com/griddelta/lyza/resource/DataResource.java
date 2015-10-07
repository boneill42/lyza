package com.griddelta.lyza.resource;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.Row;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.ConsistencyLevel;
import com.griddelta.lyza.CassandraStorage;
import com.griddelta.lyza.LyzaApplication;
import com.griddelta.lyza.LyzaConfiguration;
import com.griddelta.lyza.spark.SqlRunner;

@Path("/lyza/")
// TODO: Make consistency level configurable (HTTP Header?)
public class DataResource {
    private static Logger LOGGER = LoggerFactory.getLogger(DataResource.class);
    private LyzaApplication lyza = null;
    private LyzaConfiguration config = null;
    private SqlRunner sql;
    public static final String CONSISTENCY_LEVEL_HEADER = "X-Consistency-Level";

    public DataResource(LyzaApplication lyza) {
        this.lyza = lyza;
        this.config = lyza.getConfig();
        this.sql = new SqlRunner(config.getSparkMasterUrl(), config.getCassandraHost(), config.getCassandraPort(), "test_keyspace", "products");
    }

    // ================================================================================================================
    // Keyspace Operations
    // ================================================================================================================
    @GET
    @Path("/schema/")
    @Produces({ "application/json" })
    public JSONArray getKeyspaces() throws Exception {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Listing keyspaces.");
        return getCassandraStorage().getKeyspaces();
    }

    @PUT
    @Path("/schema/{keyspace}")
    @Produces({ "application/json" })
    public void createKeyspace(@PathParam("keyspace") String keyspace,
            @DefaultValue("SimpleStrategy") @QueryParam("strategy") String strategy,
            @DefaultValue("1") @QueryParam("replicationFactor") int replicationFactor) throws Exception {

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Creating keyspace [" + keyspace + "]");
        getCassandraStorage().addKeyspace(keyspace, strategy, replicationFactor);
    }

    @DELETE
    @Path("/schema/{keyspace}")
    @Produces({ "application/json" })
    public void dropKeyspace(@PathParam("keyspace") String keyspace) throws Exception {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Dropping keyspace [" + keyspace + "]");
        getCassandraStorage().dropKeyspace(keyspace);
    }

    // ================================================================================================================
    // Table Operations
    // ================================================================================================================
    @PUT
    @Path("/schema/{keyspace}/{table}")
    @Produces({ "application/json" })
    public void createTable(@PathParam("keyspace") String keyspace, @PathParam("table") String table,
            @QueryParam("columns") String columns, @QueryParam("keys") String keys) throws Exception {

        JSONObject columnsJson = parseJsonObject(columns);
        JSONArray keysJson = parseJsonArray(keys);
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Creating table [" + keyspace + "]:[" + table + "]" + ", columns : [" + columns + "], keys ["
                    + keys + "]");
        getCassandraStorage().createTable(keyspace, table, columnsJson, keysJson);
    }

    @DELETE
    @Path("/schema/{keyspace}/{table}")
    @Produces({ "application/json" })
    public void dropTable(@PathParam("keyspace") String keyspace, @PathParam("table") String table) throws Exception {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Deleteing table [" + keyspace + "]:[" + table + "]");
        getCassandraStorage().dropTable(keyspace, table);
    }

    // ================================================================================================================
    // Data Operations
    // ================================================================================================================
    @PUT
    @Path("/data/{keyspace}/{table}")
    @Produces({ "application/json" })
    public void update(@PathParam("keyspace") String keyspace, @PathParam("table") String table,
            @QueryParam("columns") String columns, @QueryParam("keys") String keys,
            @HeaderParam(CONSISTENCY_LEVEL_HEADER) String consistencyLevel) throws Exception {

        JSONObject columnsJson = parseJsonObject(columns);
        JSONObject keysJson = parseJsonObject(keys);
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Update [" + keyspace + "]:[" + table + "]" + ", columns : [" + columns + "], keys : [" + keys
                    + "]");

        getCassandraStorage().update(keyspace, table, columnsJson, keysJson, getConsistencyLevel(consistencyLevel));
    }

    @GET
    @Path("/data/{keyspace}/{table}")
    public JSONArray select(@PathParam("keyspace") String keyspace, @PathParam("table") String table,
            @QueryParam("columns") String columns, @QueryParam("where") String where,
            @HeaderParam(CONSISTENCY_LEVEL_HEADER) String consistencyLevel) throws Exception {

        JSONArray columnsJson = parseJsonArray(columns);
        JSONObject whereJson = parseJsonObject(where);
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Select [" + keyspace + "]:[" + table + "], columns: [" + columns + "], where : [" + where
                    + "]");

        return getCassandraStorage().select(keyspace, table, columnsJson, whereJson, config.getConsistencyLevel());
    }

    @DELETE
    @Path("/data/{keyspace}/{table}")
    @Produces({ "application/json" })
    public void delete(@PathParam("keyspace") String keyspace, @PathParam("table") String table,
            @QueryParam("columns") String columns, @QueryParam("where") String where,
            @HeaderParam(CONSISTENCY_LEVEL_HEADER) String consistencyLevel) throws Exception {
        JSONObject whereJson = parseJsonObject(where);
        JSONArray columnsJson = parseJsonArray(columns);
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Deleting [" + keyspace + "]:[" + table + "], columns: [" + columns + "], where [" + where
                    + "]");

        getCassandraStorage().delete(keyspace, table, columnsJson, whereJson, config.getConsistencyLevel());
    }

    // ================================================================================================================
    // SQL Operations
    // ================================================================================================================

    @POST
    @Path("/sql/{keyspace}/{table}")
    public String sql(@PathParam("keyspace") String keyspace, @PathParam("table") String table,
            @QueryParam("sql") String query) throws Exception {        
        DataFrame dataFrame = sql.query(query);        
        String responseData = dataFrameToJson(dataFrame).toJSONString();
        LOGGER.debug(responseData);
        return responseData;
    }

    // ================================================================================================================
    // Helper Methods
    // ================================================================================================================

    public ConsistencyLevel getConsistencyLevel(String consistencyLevel) {
        if (consistencyLevel == null)
            return null;
        else
            return ConsistencyLevel.valueOf(consistencyLevel);
    }

    public CassandraStorage getCassandraStorage() {
        return this.lyza.getStorage();
    }

    private JSONArray parseJsonArray(String str) {
        JSONArray json;
        if (str != null) {
            json = (JSONArray) JSONValue.parse(str);
        } else {
            json = null;
        }
        return json;
    }

    private JSONObject parseJsonObject(String str) {
        JSONObject json;
        if (str != null) {
            json = (JSONObject) JSONValue.parse(str);
        } else {
            json = null;
        }
        return json;
    }
    
    @SuppressWarnings("unchecked")
    private JSONArray dataFrameToJson(DataFrame dataFrame){
        JSONArray result = new JSONArray();
        for (Row row : dataFrame.collect()){
            JSONArray rowArray = new JSONArray();
            for (int i=0; i < row.size(); i++){
                rowArray.add(row.get(i));
            }
            result.add(rowArray);
        }
        return result;
    }

}
