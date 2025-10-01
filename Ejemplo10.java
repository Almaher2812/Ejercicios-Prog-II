import java.util.*;

/**
 * UNIVERSIDAD: Agregación de servicios (registro+cartera) + paginación de cursos.
 * Login: admin/1234, user/abcd
 *
 * Demuestra:
 * - Orquestación/Agregación: un endpoint llama a dos servicios internos y combina respuestas
 * - Paginación con offset/limit
 */
interface Service10 { String handle(String data, String user); }

/** Login simple */
class LoginService10 {
    private final Map<String,String> users = Map.of("admin","1234","user","abcd");
    boolean login(String u,String p){ return users.containsKey(u)&&users.get(u).equals(p); }
}

/** Gateway con rutas de negocio universitario */
class ApiGateway10 {
    private final LoginService10 login;
    private final Map<String,Service10> routes = new HashMap<>();
    ApiGateway10(LoginService10 l){ this.login=l; }
    void register(String p, Service10 s){ routes.put(p,s); }
    String request(String u,String p,String path,String data){
        if(!login.login(u,p)) return "❌ Acceso denegado.";
        Service10 s = routes.get(path);
        if(s==null) return "❌ Ruta no encontrada.";
        return s.handle(data,u);
    }
}

public class Ejemplo10Universidad {
    public static void main(String[] args){
        LoginService10 ls = new LoginService10();
        ApiGateway10 gw = new ApiGateway10(ls);

        // Catálogo de cursos para paginación
        List<String> cursos = Arrays.asList(
            "Programación I","Programación II","Estructuras","Análisis",
            "Bases de Datos","Redes","Sistemas Operativos","IA",
            "Minería de Datos","Seguridad","DevOps","Arquitectura",
            "Microservicios","Cloud","Calidad de Software"
        );

        // Servicios internos simulados
        Service10 registro = (data,user)->"Registro OK para " + data + " ["+user+"]";
        Service10 cartera  = (data,user)->"Factura generada para " + data;

        // Agregación: inscripción llama a registro y cartera
        gw.register("/uni/inscribir",(data,user)-> registro.handle(data,user) + " | " + cartera.handle(data,user));

        // Paginación: data = "offset,limit"
        gw.register("/uni/cursos",(data,user)->{
            String[] p = data.split(",");
            int off = Integer.parseInt(p[0].trim());
            int lim = Integer.parseInt(p[1].trim());
            int end = Math.min(off+lim, cursos.size());
            if(off>=cursos.size()) return "[] (página vacía)";
            return cursos.subList(off,end).toString();
        });

        // Flujo de prueba
        System.out.println(gw.request("user","abcd","/uni/inscribir","Programación II"));
        System.out.println(gw.request("user","abcd","/uni/cursos","0,5"));
        System.out.println(gw.request("user","abcd","/uni/cursos","5,5"));
        System.out.println(gw.request("user","abcd","/uni/cursos","10,5"));
    }
}
// EOF
