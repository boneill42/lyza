package com.griddelta.lyza.spark;

import java.util.Random;

import org.junit.Ignore;
import org.junit.Test;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

@Ignore
public class SqlTest {

    public static void generateData() {
        Cluster cluster = Cluster.builder().addContactPoints("localhost").build();
        Session session = cluster.connect();
        session.execute("DROP KEYSPACE IF EXISTS test_keyspace");
        session.execute("CREATE KEYSPACE test_keyspace WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}");
        session.execute("CREATE TABLE test_keyspace.products (id INT PRIMARY KEY, name TEXT, price FLOAT, )");
        
        Random random = new Random();
        
        for (int i = 0; i < 10; i++) {
            String cql = "INSERT INTO test_keyspace.products (id, name, price) values (" + i + ",'" + "name" + i + "'," + random.nextDouble() + ")";
            System.out.println(cql);
            session.execute(cql);
        }
    }

    @Test
    public void testMe() {
        generateData();
        String[] args = {"local", "localhost", "9042", "test_keyspace", "products"};
        SqlRunner.main(args);
    }
}