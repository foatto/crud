package foatto.sql

enum class SQLDialect( val dialect: String,
                       //--- наименование целочисленного типа
                       val integerFieldTypeName: String,
                       //--- поддержка полей с бинарными данными
                       val isBinaryDataSupported: Boolean,
                       //--- наименование типа поля для хранения hex-данных (чисто бинарное или эмулирующее его текстовое)
                       val hexFieldTypeName: String,
                       //--- максимальный размер поля с бинарными данными
                       val binaryFieldMaxSize: Int,
                       //--- максимальный размер текстового поля
                       val textFieldMaxSize: Int,
                       //--- возможность создания кластерного индекса
                       val createClusteredIndex: String ) {

    //--- jdbc:h2:tcp://localhost//$root_dir$/mms
    //--- java -cp *.jar org.h2.tools.Server
    // ? no online backup
    // - no Multi-Threaded Statement Processing
    H2( "H2",
        " INT ",                // INTEGER, INT4, MEDIUMNINT
        true,                   // binary data supported
        " BYTEA ",              // можно ((LONG)VAR)BINARY,RAW,BYTEA
        64000,                  // до 2 GB ); от греха подальше сделаем 64К//
        64000,                  // Integer.MAX_VALUE ); от греха подальше сделаем 64К//
        " CREATE INDEX " ),

    //--- jdbc:sqlserver://localhost;databaseName=mms;selectMethod=cursor;
    // + clustered index
    MSSQL( "MSSQL",
           " INT ",
           true,
           " VARBINARY( 8000 ) ",
           8000,
           8000,
           " CREATE CLUSTERED INDEX " ),

    //--- smallest max length of VARCHAR field (4000 chars)
    ORACLE( "Oracle",
            " NUMBER( 10 ) ",
            false,
            " VARCHAR2( 4000 ) ",
            4000,
            4000,
            " CREATE INDEX " ),

    //--- jdbc:postgresql://localhost/mms
    POSTGRESQL( "PostgreSQL",
                " INT ",
                true,
                " BYTEA ",           // Integer.MAX_VALUE ); от греха подальше сделаем 64К
                64000,            // Integer.MAX_VALUE ); от греха подальше сделаем 64К
                64000,            // Integer.MAX_VALUE ); от греха подальше сделаем 64К
                " CREATE INDEX " ),

    //--- jdbc:sqlite:$root_dir$/mms.sqlite
    // - no client/server mode, embedded/file mode only
    // - no online backup, file copy of closed base only
    // - native modules in jdbc drivers (bad support for ARM platforms)
    // - one writer & many reader only (in WAL mode)
    // - no ALTER TABLE (except ADD) & ALTER INDEX statements
    SQLITE( "SQLite",
            " INTEGER ",
            false,
            " TEXT ",         // 1_000_000_000 ); от греха подальше сделаем 64К
            64000,            // 1_000_000_000 ); от греха подальше сделаем 64К
            64000,            // 1_000_000_000 ); от греха подальше сделаем 64К
            " CREATE INDEX " );

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    companion object {
        //--- для поиска описания диалекта по строковому имени
        val hmDialect = mapOf( H2.dialect to H2, MSSQL.dialect to MSSQL, ORACLE.dialect to ORACLE, POSTGRESQL.dialect to POSTGRESQL, SQLITE.dialect to SQLITE)
    }

}

