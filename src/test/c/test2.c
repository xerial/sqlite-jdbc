#ifndef SQLITE_CORE
  #include "sqlite3ext.h"
  SQLITE_EXTENSION_INIT1
#else
  #include "sqlite3.h"
#endif

static void test2(
  sqlite3_context *context, 
  int argc, 
  sqlite3_value **argv)
{
  sqlite3_result_int(context, 1);
}

#if !SQLITE_CORE
#ifdef _WIN32
__declspec(dllexport)
#endif
int sqlite3_test2_init(
  sqlite3 *db, 
  char **pzErrMsg,
  const sqlite3_api_routines *pApi)
{
  SQLITE_EXTENSION_INIT2(pApi)
  return sqlite3_create_function(
        db, "test2", 0, SQLITE_ANY, (void*)0,
        test2, 0, 0);
}
#endif
