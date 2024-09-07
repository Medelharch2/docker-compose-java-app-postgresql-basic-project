import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import com.sun.net.httpserver.*;

public class SimpleServer {
    private static final String DB_URL = "jdbc:postgresql://db:5432/mydatabase";
    private static final String DB_USER = "myuser";
    private static final String DB_PASSWORD = "mypassword";

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8083), 0);
        server.createContext("/", new LoginHandler());
        server.createContext("/user", new UserPageHandler());
        server.start();
        System.out.println("Server started on port 8083");
    }

    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                BufferedReader reader = new BufferedReader(isr);
                String query = reader.readLine();
                Map<String, String> params = queryToMap(query);

                String email = params.get("email");
                String password = params.get("password");

                storeLoginData(email, password);

                String response = "<html><body><h1>Login Successful</h1><p>Welcome, " + email + "!</p><a href=\"/user\">Go to User Page</a></body></html>";
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes(StandardCharsets.UTF_8));
                os.close();
            } else {
                serveLoginPage(exchange);
            }
        }

        private Map<String, String> queryToMap(String query) {
            Map<String, String> map = new HashMap<>();
            if (query != null) {
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    String[] keyValue = pair.split("=");
                    if (keyValue.length == 2) {
                        map.put(URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8), URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8));
                    }
                }
            }
            return map;
        }

        private void storeLoginData(String email, String password) {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String sql = "INSERT INTO logins (email, password) VALUES (?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, email);
                    stmt.setString(2, password);
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private void serveLoginPage(HttpExchange exchange) throws IOException {
            String response = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Login Page</title>
                    <style>
                        body { font-family: Arial, sans-serif; }
                        .container { max-width: 600px; margin: auto; padding: 20px; }
                        h1 { color: #333; }
                        form { display: flex; flex-direction: column; }
                        label { margin: 10px 0 5px; }
                        input { padding: 10px; margin-bottom: 10px; border: 1px solid #ccc; border-radius: 5px; }
                        input[type="submit"] { background-color: #4CAF50; color: white; border: none; cursor: pointer; }
                        input[type="submit"]:hover { background-color: #45a049; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>Login</h1>
                        <form action="/" method="post">
                            <label for="email">Email:</label>
                            <input type="email" id="email" name="email" required>
                            <label for="password">Password:</label>
                            <input type="password" id="password" name="password" required>
                            <input type="submit" value="Login">
                        </form>
                    </div>
                </body>
                </html>
            """;
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.close();
        }
    }

    static class UserPageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>User Page</title>
                    <style>
                        body { font-family: Arial, sans-serif; }
                        .container { max-width: 600px; margin: auto; padding: 20px; }
                        h1 { color: #333; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>Welcome to the User Page!</h1>
                        <p>This is a placeholder for user-specific content.</p>
                        <a href="/">Go to Login Page</a>
                    </div>
                </body>
                </html>
            """;
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.close();
        }
    }
}

