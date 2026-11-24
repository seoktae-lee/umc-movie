import os
import pymysql

connection = pymysql.connect(host=os.environ['DB_HOST'], user='admin', password=os.environ['DB_PASSWORD'], connect_timeout=10)
with connection.cursor() as cursor:
    cursor.execute("CREATE USER IF NOT EXISTS 'umc_exporter'@'%%' IDENTIFIED BY %s WITH MAX_USER_CONNECTIONS 3", (os.environ['EXPORTER_PASSWORD'],))
    cursor.execute("GRANT PROCESS, REPLICATION CLIENT ON *.* TO 'umc_exporter'@'%'")
    cursor.execute("GRANT SELECT ON performance_schema.* TO 'umc_exporter'@'%'")
connection.close()
check = pymysql.connect(host=os.environ['DB_HOST'], user='umc_exporter', password=os.environ['EXPORTER_PASSWORD'], connect_timeout=10)
with check.cursor() as cursor:
    cursor.execute("SHOW GLOBAL STATUS WHERE Variable_name IN ('Threads_connected','Threads_running')")
    print(cursor.fetchall())
check.close()
