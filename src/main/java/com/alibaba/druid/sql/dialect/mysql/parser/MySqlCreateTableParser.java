/*
 * Copyright 1999-2011 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.druid.sql.dialect.mysql.parser;

import com.alibaba.druid.sql.ast.SQLName;
import com.alibaba.druid.sql.ast.statement.SQLColumnDefinition;
import com.alibaba.druid.sql.ast.statement.SQLCreateTableStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLTableConstaint;
import com.alibaba.druid.sql.dialect.mysql.ast.MySqlKey;
import com.alibaba.druid.sql.dialect.mysql.ast.MySqlPrimaryKey;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlCreateTableStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlPartitionByKey;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlTableIndex;
import com.alibaba.druid.sql.parser.Lexer;
import com.alibaba.druid.sql.parser.ParserException;
import com.alibaba.druid.sql.parser.SQLCreateTableParser;
import com.alibaba.druid.sql.parser.Token;

public class MySqlCreateTableParser extends SQLCreateTableParser {

    public MySqlCreateTableParser(String sql) throws ParserException{
        super(sql);
        this.exprParser = new MySqlExprParser(lexer);
    }

    public MySqlCreateTableParser(Lexer lexer){
        super(lexer);
        this.exprParser = new MySqlExprParser(lexer);
    }

    public SQLCreateTableStatement parseCrateTable() throws ParserException {
        return parseCrateTable(true);
    }

    public SQLCreateTableStatement parseCrateTable(boolean acceptCreate) throws ParserException {
        if (acceptCreate) {
            accept(Token.CREATE);
        }
        MySqlCreateTableStatement stmt = new MySqlCreateTableStatement();

        if (identifierEquals("TEMPORARY")) {
            lexer.nextToken();
            stmt.setType(SQLCreateTableStatement.Type.GLOBAL_TEMPORARY);
        }

        accept(Token.TABLE);

        if (identifierEquals("IF")) {
            lexer.nextToken();
            accept(Token.NOT);
            accept(Token.EXISTS);

            stmt.setIfNotExiists(true);
        }

        stmt.setName(this.exprParser.name());

        if (lexer.token() == (Token.LPAREN)) {
            lexer.nextToken();

            for (;;) {
                if (lexer.token() == Token.IDENTIFIER) {
                    SQLColumnDefinition column = this.exprParser.parseColumn();
                    stmt.getTableElementList().add(column);
                } else if (lexer.token() == (Token.CONSTRAINT)) {
                    stmt.getTableElementList().add(parseConstraint());
                } else if (lexer.token() == (Token.INDEX)) {
                    lexer.nextToken();

                    MySqlTableIndex idx = new MySqlTableIndex();

                    if (lexer.token() == Token.IDENTIFIER) {
                        if (!"USING".equalsIgnoreCase(lexer.stringVal())) {
                            idx.setName(this.exprParser.name());
                        }
                    }

                    if (identifierEquals("USING")) {
                        lexer.nextToken();
                        idx.setIndexType(lexer.stringVal());
                        lexer.nextToken();
                    }

                    accept(Token.LPAREN);
                    for (;;) {
                        idx.getColumns().add(this.exprParser.expr());
                        if (!(lexer.token() == (Token.COMMA))) {
                            break;
                        } else {
                            lexer.nextToken();
                        }
                    }
                    accept(Token.RPAREN);

                    stmt.getTableElementList().add(idx);
                } else if (lexer.token() == (Token.KEY)) {
                    stmt.getTableElementList().add(parseConstraint());
                } else if (lexer.token() == (Token.PRIMARY)) {
                    stmt.getTableElementList().add(parseConstraint());
                }

                if (!(lexer.token() == (Token.COMMA))) {
                    break;
                } else {
                    lexer.nextToken();
                }
            }

            accept(Token.RPAREN);
        }

        for (;;) {
            if (identifierEquals("ENGINE")) {
                lexer.nextToken();
                accept(Token.EQ);
                stmt.getTableOptions().put("ENGINE", lexer.stringVal());
                lexer.nextToken();
                continue;
            }

            if (identifierEquals("TYPE")) {
                lexer.nextToken();
                accept(Token.EQ);
                stmt.getTableOptions().put("TYPE", lexer.stringVal());
                lexer.nextToken();
                continue;
            }

            if (identifierEquals("PARTITION")) {
                lexer.nextToken();
                accept(Token.BY);

                if (lexer.token() == Token.KEY) {
                    MySqlPartitionByKey clause = new MySqlPartitionByKey();
                    lexer.nextToken();
                    accept(Token.LPAREN);
                    for (;;) {
                        clause.getColumns().add(this.exprParser.name());
                        if (lexer.token() == Token.COMMA) {
                            lexer.nextToken();
                            continue;
                        }
                        break;
                    }
                    accept(Token.RPAREN);
                    stmt.setPartitioning(clause);
                    
                    if (identifierEquals("PARTITIONS")) {
                        lexer.nextToken();
                        clause.setPartitionCount(this.exprParser.expr());
                    }
                } else {
                    throw new ParserException("TODO " + lexer.token() + " " + lexer.stringVal());
                }
            }

            break;
        }

        if (lexer.token() == (Token.ON)) {
            throw new ParserException("TODO");
        }

        if (lexer.token() == (Token.SELECT)) {
            SQLSelect query = new MySqlSelectParser(this.lexer).select();
            stmt.setQuery(query);
        }

        return stmt;
    }

    @SuppressWarnings("unused")
    protected SQLTableConstaint parseConstraint() throws ParserException {
        SQLName name = null;
        if (lexer.token() == (Token.CONSTRAINT)) {
            lexer.nextToken();
        }

        if (lexer.token() == Token.IDENTIFIER) {
            name = this.exprParser.name();
        }

        if (lexer.token() == (Token.KEY)) {
            lexer.nextToken();

            MySqlKey key = new MySqlKey();

            if (identifierEquals("USING")) {
                lexer.nextToken();
                key.setIndexType(lexer.stringVal());
                lexer.nextToken();
            }

            accept(Token.LPAREN);
            for (;;) {
                key.getColumns().add(this.exprParser.expr());
                if (!(lexer.token() == (Token.COMMA))) {
                    break;
                } else {
                    lexer.nextToken();
                }
            }
            accept(Token.RPAREN);

            return key;
        }

        if (lexer.token() == (Token.PRIMARY)) {
            lexer.nextToken();
            accept(Token.KEY);

            MySqlPrimaryKey primaryKey = new MySqlPrimaryKey();

            if (identifierEquals("USING")) {
                lexer.nextToken();
                primaryKey.setIndexType(lexer.stringVal());
                lexer.nextToken();
            }

            accept(Token.LPAREN);
            for (;;) {
                primaryKey.getColumns().add(this.exprParser.expr());
                if (!(lexer.token() == (Token.COMMA))) {
                    break;
                } else {
                    lexer.nextToken();
                }
            }
            accept(Token.RPAREN);

            return primaryKey;
        }

        throw new ParserException("TODO");
    }
}