package com.legacybridge.auth;

import com.legacybridge.auth.entity.Role;
import com.legacybridge.auth.entity.User;
import com.legacybridge.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * SOLO PARA DESARROLLO/DEMO. Este proyecto todavia no tiene un endpoint
 * de registro, asi que al arrancar se crean (si no existen) un usuario
 * ROLE_USER y uno ROLE_ADMIN para poder probar el login. Un CommandLineRunner
 * es intencionalmente la pieza mas simple posible para esto: no justifica
 * un modulo de "registration" completo en esta fase.
 *
 * NUNCA hacer esto en produccion.
 */
@Component
public class DemoUserSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoUserSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoUserSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        crearSiNoExiste("user.demo", "User#12345", Role.ROLE_USER);
        crearSiNoExiste("admin.demo", "Admin#12345", Role.ROLE_ADMIN);
    }

    private void crearSiNoExiste(String username, String rawPassword, Role role) {
        if (userRepository.findByUsername(username).isPresent()) {
            return;
        }
        User user = new User(username, passwordEncoder.encode(rawPassword), role);
        userRepository.save(user);
        log.info("[DEMO] Usuario creado -> username={} password={} role={} (SOLO DESARROLLO)",
                username, rawPassword, role);
    }
}
