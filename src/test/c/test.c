#ifndef SQLITE_CORE
  #include "sqlite3ext.h"
  SQLITE_EXTENSION_INIT1
#else
  #include "sqlite3.h"
#endif

static void test(
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
int sqlite3_test_init(
  sqlite3 *db, 
  char **pzErrMsg,
  const sqlite3_api_routines *pApi)
{
  SQLITE_EXTENSION_INIT2(pApi)
  return sqlite3_create_function(
        db, "test", 0, SQLITE_ANY, (void*)0,
        test, 0, 0);
}
#endif

#if !SQLITE_CORE
#ifdef _WIN32
__declspec(dllexport)
#endif
int sqlite3_testa_init(
  sqlite3 *db, 
  char **pzErrMsg,
  const sqlite3_api_routines *pApi)
{
  SQLITE_EXTENSION_INIT2(pApi)
  return sqlite3_create_function(
        db, "testa", 0, SQLITE_ANY, (void*)0,
        test, 0, 0);
}
#endif
