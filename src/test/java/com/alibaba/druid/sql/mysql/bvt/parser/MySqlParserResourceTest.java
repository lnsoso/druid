package com.alibaba.druid.sql.mysql.bvt.parser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlOutputVisitor;
import com.alibaba.druid.util.JdbcUtils;

public class MySqlParserResourceTest extends TestCase {

    public void test_0() throws Exception {
//        exec_test("bvt/parser/mysql-0.txt");
//        exec_test("bvt/parser/mysql-1.txt");
//        exec_test("bvt/parser/mysql-2.txt");
//        exec_test("bvt/parser/mysql-3.txt");
//        exec_test("bvt/parser/mysql-4.txt");
//        exec_test("bvt/parser/mysql-5.txt");
//        exec_test("bvt/parser/mysql-6.txt");
//        exec_test("bvt/parser/mysql-7.txt");
        exec_test("bvt/parser/mysql-8.txt");
    }
    
    
    public void exec_test(String resource) throws Exception {
        System.out.println(resource);
        InputStream is = null;

        is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        Reader reader = new InputStreamReader(is, "UTF-8");
        String input = JdbcUtils.read(reader);
        JdbcUtils.close(reader);
        String[] items = input.split("---------------------------");
        String sql = items[0].trim();
        String expect = items[1].trim();
        
        MySqlStatementParser parser = new MySqlStatementParser(sql);
        List<SQLStatement> statementList = parser.parseStatementList();
        
        Assert.assertEquals(1, statementList.size());
        
        String text = output(statementList);
        System.out.println(text);
        Assert.assertEquals(expect, text.trim());
       
    }

    private String output(List<SQLStatement> stmtList) {
        StringBuilder out = new StringBuilder();
        MySqlOutputVisitor visitor = new MySqlOutputVisitor(out);

        for (SQLStatement stmt : stmtList) {
            stmt.accept(visitor);
        }

        return out.toString();
    }
}