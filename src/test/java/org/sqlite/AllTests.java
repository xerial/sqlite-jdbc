package org.sqlite;

import org.junit.runners.Suite;
import org.junit.runner.RunWith;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    BackupTest.class,
    ConnectionTest.class,
    DBMetaDataTest.class,
    ExtendedCommandTest.class,
    ExtensionTest.class,
    FetchSizeTest.class,
    InsertQueryTest.class,
    JDBCTest.class,
    OSInfoTest.class,
    PrepStmtTest.class,
    QueryTest.class,
    ReadUncommittedTest.class,
    RSMetaDataTest.class,
    SQLiteDataSourceTest.class,
    SQLiteJDBCLoaderTest.class,
    SQLitePureJavaTest.class,
    StatementTest.class,
    TransactionTest.class,
    UDFTest.class
})
public class AllTests {
// runs all Tests
}
