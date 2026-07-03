import java.sql.Connection;
import java.sql.DriverManager;

public class FindMySQLPassword {
    public static void main(String[] args) {
        String[] passwords = {
            "", "root", "root123", "password", "123456", "admin", 
            "mysql", "vibe123", "admin123", "12345678", "vibe", 
            "123456789", "qwe123", "zxcv123", "asdf123", 
            "123", "abc123", "welcome", "test", "guest",
            "master", "slave", "database", "server", "oracle",
            "postgres", "sql", "123123", "000000", "888888",
            "666666", "111111", "123qwe", "qwerty", "abcdef",
            "abcd1234", "password1", "admin1234", "rootroot",
            "toor", "changeme", "letmein", "password123", "1234567"
        };
        
        String url = "jdbc:mysql://localhost:3306/mysql?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
        String user = "root";
        
        for (String pwd : passwords) {
            try {
                Connection conn = DriverManager.getConnection(url, user, pwd);
                System.out.println("SUCCESS! Password found: '" + pwd + "'");
                conn.close();
                return;
            } catch (Exception e) {
                System.out.println("Failed with password: '" + pwd + "'");
            }
        }
        System.out.println("No password found in the list.");
    }
}
