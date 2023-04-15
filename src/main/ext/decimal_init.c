int core_init(const char* dummy)
{
    int nErr = 0;

    nErr += sqlite3_auto_extension((void*)sqlite3_decimal_init);
    // repeat above for any other extensions you append and want to init on each connection
    return nErr ? SQLITE_ERROR : SQLITE_OK;
}
