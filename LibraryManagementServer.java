import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class LibraryManagementServer {

    static class Book {
        String id, title, author;
        Book(String id, String title, String author) {
            this.id = id;
            this.title = title;
            this.author = author;
        }
    }

    static ArrayList<Book> books = new ArrayList<>();
    static boolean isLoggedIn = false;

    public static void main(String[] args) throws Exception {

        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);

        server.createContext("/", exchange -> {
            if (!isLoggedIn) {
                sendResponse(exchange, getLoginPage());
            } else {
                sendResponse(exchange, getHomePage());
            }
        });

        server.createContext("/login", exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                String formData = new String(exchange.getRequestBody().readAllBytes());
                Map<String, String> data = parseFormData(formData);

                String username = data.get("username");
                String password = data.get("password");

                if ("admin".equals(username) && "1234".equals(password)) {
                    isLoggedIn = true;
                }

                exchange.getResponseHeaders().add("Location", "/");
                exchange.sendResponseHeaders(302, -1);
            }
        });

            server.createContext("/logout", exchange -> {
            isLoggedIn = false;

            exchange.getResponseHeaders().add("Location", "/");
            exchange.sendResponseHeaders(302, -1);
                exchange.close();
});

        server.createContext("/add", exchange -> {
            if (isLoggedIn && "POST".equals(exchange.getRequestMethod())) {
                String formData = new String(exchange.getRequestBody().readAllBytes());
                Map<String, String> data = parseFormData(formData);

                books.add(new Book(
                        data.get("id"),
                        data.get("title"),
                        data.get("author")
                        
                ));
            }
            exchange.getResponseHeaders().add("Location", "/");
            exchange.sendResponseHeaders(302, -1);
        });

        server.createContext("/delete", exchange -> {
            if (isLoggedIn) {
                String query = exchange.getRequestURI().getQuery();
                if (query != null && query.startsWith("id=")) {
                    String id = query.substring(3);
                    books.removeIf(book -> book.id.equals(id));
                }
            }
            exchange.getResponseHeaders().add("Location", "/");
            exchange.sendResponseHeaders(302, -1);
        });

        

        server.start();
        System.out.println("Server started at http://localhost:8000");
    }

    private static String getLoginPage() {
        return """
            <html>
            <head>
                <title>Admin Login</title>
                <style>
                    body { font-family: Arial; background: #f4f6f9; text-align: center; }
                    .box { width: 300px; margin: 100px auto; background: white; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px #ccc; }
                    input { width: 100%; padding: 8px; margin: 10px 0; }
                    button { padding: 8px 15px; background: #2c3e50; color: white; border: none; }
                </style>
            </head>
            <body>
                <div class='box'>
                    <h2>Admin Login</h2>
                    <form method='post' action='/login'>
                        <input type='text' name='username' placeholder='Username' required>
                        <input type='password' name='password' placeholder='Password' required>
                        <button type='submit'>Login</button>
                    </form>
                </div>
            </body>
            </html>
        """;
    }

    private static String getHomePage() {

        StringBuilder rows = new StringBuilder();
        for (Book b : books) {
            rows.append("<tr>")
                .append("<td>").append(b.id).append("</td>")
                .append("<td>").append(b.title).append("</td>")
                .append("<td>").append(b.author).append("</td>")
                .append("<td><a style='color:red' href='/delete?id=")
                .append(b.id)
                .append("'>Delete</a></td>")
                .append("</tr>");
        }

        return """
            <html>
            <head>
                <title>Library Management</title>
                <style>
                    body { font-family: Arial; background: #f4f6f9; text-align: center; }

                    h1 { background: #2c3e50; color: white; padding: 15px; }

                    .container { width: 70%; margin: auto; background: white; padding: 20px; border-radius: 8px; }

                    input { padding: 8px; margin: 5px; }

                    button { padding: 8px 15px; background: #27ae60; color: white; border: none; }

                    table { width: 100%; border-collapse: collapse; margin-top: 20px; }

                    th, td { border: 1px solid #ddd; padding: 10px; }

                    th { background: #34495e; color: white; }

                    .logout { float: right; margin-top: -50px; margin-right: 20px; color: white}
                </style>
            </head>
            <body>
                <h1>Library Management System</h1>
                <a class='logout' href='/logout'>Logout</a>
                <div class='container'>
                    <h2>Add Book</h2>
                    <form method='post' action='/add'>
                        <input type='text' name='id' placeholder='Book ID' required>
                        <input type='text' name='title' placeholder='Title' required>
                        <input type='text' name='author' placeholder='Author' required>
                        <button type='submit'>Add Book</button>
                    </form>

                    <h2>Book List</h2>
                    <table>
                        <tr>
                            <th>ID</th>
                            <th>Title</th>
                            <th>Author</th>
                            <th>Action</th>
                        </tr>
        """ + rows + """
                    </table>
                </div>
            </body>
            </html>
        """;
    }

    private static Map<String, String> parseFormData(String formData) throws UnsupportedEncodingException {
        Map<String, String> map = new HashMap<>();
        for (String pair : formData.split("&")) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                map.put(URLDecoder.decode(keyValue[0], "UTF-8"),
                        URLDecoder.decode(keyValue[1], "UTF-8"));
            }
        }
        return map;
    }

    private static void sendResponse(HttpExchange exchange, String response) throws IOException {
        exchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}