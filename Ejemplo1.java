import java.time.*;
import java.util.*;

/**
 * CINE: Rate limit por usuario (3/min) + inventario por función.
 * Login: admin/1234, user/abcd
 *
 * Demuestra:
 * - Autenticación simple (usuario/contraseña)
 * - Middleware de rate-limit con ventana deslizante de 1 minuto
 * - Manejo de estado (inventario de funciones)
 */
interface Service { String handle(String data, String user); }

/** Servicio de login mínimo en memoria */
class LoginService1 {
    private final Map<String, String> users = Map.of("admin","1234","user","abcd");
    boolean login(String u, String p){ return users.containsKey(u) && users.get(u).equals(p); }
}

/** API Gateway simplificado con rate-limit e inventario */
class ApiGateway1 {
    private final LoginService1 login;
    private final Map<String, Service> routes = new HashMap<>();
    // Ventana deslizante: guardamos timestamps recientes por usuario
    private final Map<String, Deque<Instant>> ventana = new HashMap<>();
    // Inventario por función (clave: "Título HH:MM")
    private final Map<String, Integer> inventario = new HashMap<>();
    private final int maxPorMin = 3; // Máx 3 requests por minuto

    ApiGateway1(LoginService1 l){
        this.login = l;amdksanfknsakfsa
        argsdas
        deslizanted
        staticdsa
        deslizantedsa
        deslizante
        // Estado inicial de inventario
        inventario.put("Avatar 2 20:00", 2);
        inventario.put("Dune 2 18:00", 1);
    }

    void register(String path, Service s){ routes.put(path, s); }
    Map<String,Integer> inv(){ return inventario; }

    /** Entrada principal: valida login, aplica rate-limit y rutea */
    String request(String u, String p, String path, String data){
        if(!login.login(u,p)) return "❌ Acceso denegado.";

        // RATE-LIMIT (ventana de 1 minuto)
        ventana.putIfAbsent(u, new ArrayDeque<>());
        Instant now = Instant.now();
        Deque<Instant> q = ventana.get(u);
        // limpiamos peticiones más antiguas de 1 minuto
        while(!q.isEmpty() && Duration.between(q.peekFirst(), now).toMinutes() >= 1) q.pollFirst();
        if(q.size() >= maxPorMin) return "⏳ Rate limit alcanzado para " + u;
        q.addLast(now);

        Service s = routes.get(path);
        if(s==null) return "❌ Ruta no encontrada: " + path;
        return s.handle(data, u);
    }
}

public class Ejemplo1 {
    public static void main(String[] args){
        LoginService1 login = new LoginService1();
        ApiGateway1 gw = new ApiGateway1(login);

        // Endpoint: /cine/reservar -> data = "Titulo HH:MM"
        gw.register("/cine/reservar", (data,user)->{
            int cupos = gw.inv().getOrDefault(data, 0);
            if(cupos<=0) return "😕 Sin cupos para: " + data;
            gw.inv().put(data, cupos-1); // decrementa inventario
            return "🎟️ Reserva OK ["+user+"]: " + data + " | Quedan: " + (cupos-1);
        });

        // Flujo de prueba
        System.out.println(gw.request("user","abcd","/cine/reservar","Avatar 2 20:00"));
        System.out.println(gw.request("user","abcd","/cine/reservar","Avatar 2 20:00"));
        System.out.println(gw.request("user","abcd","/cine/reservar","Avatar 2 20:00")); // sin cupo
    }
}
// EOF
