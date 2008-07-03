/*
 * Copyright (c) 2007 David Crawshaw <david@zentus.com>
 * 
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

#include <stdlib.h>
#include "sqlite3.h"

/* Provides access to metadata across NestedVM 7-argument limit on functions.*/
struct metadata {
  int pNotNull;
  int pPrimaryKey;
  int pAutoinc;
};

int column_metadata_helper(
  sqlite3 *db,
  sqlite3_stmt *stmt,
  int col,
  struct metadata *p
){
  const char *zTableName, *zColumnName;
  int rc = 0;

  p->pNotNull = 0;
  p->pPrimaryKey = 0;
  p->pAutoinc = 0;

  zTableName = sqlite3_column_table_name(stmt, col);
  zColumnName = sqlite3_column_name(stmt, col);

  if (zTableName && zColumnName) {
    rc = sqlite3_table_column_metadata(
      db, 0, zTableName, zColumnName, 0, 0,
      &p->pNotNull, &p->pPrimaryKey, &p->pAutoinc
    );
  }

  return rc;
}


extern int _call_java(int xType, int context, int args, int value);

void xFunc_helper(sqlite3_context *context, int args, sqlite3_value** value)
{
    _call_java(1, (int)context, args, (int)value);
}

void xStep_helper(sqlite3_context *context, int args, sqlite3_value** value)
{
    _call_java(2, (int)context, args, (int)value);
}

void xFinal_helper(sqlite3_context *context)
{
    _call_java(3, (int)context, 0, 0);
}

/* create function if pos is non-negative, aggregate if agg is true */
int create_function_helper(sqlite3 *db, const char *name, int pos, int agg)
{
    return sqlite3_create_function(db, name, -1, SQLITE_ANY, (void*)pos,
            pos>=0 && !agg ? &xFunc_helper : 0,
            pos>=0 &&  agg ? &xStep_helper : 0,
            pos>=0 &&  agg ? &xFinal_helper : 0);
}
