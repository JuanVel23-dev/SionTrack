package com.siontrack.siontrack.services;

import java.time.LocalDateTime;
import java.util.Collections;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.siontrack.siontrack.models.Usuarios;
import com.siontrack.siontrack.repository.UsuariosRepository;

/**
 * Implementación de {@link UserDetailsService} para la autenticación con Spring Security.
 *
 * <p>Carga el usuario por nombre de usuario, verifica que esté activo y
 * actualiza su campo {@code ultimoAcceso} en cada inicio de sesión exitoso.
 * El rol del usuario se expone como una autoridad con el prefijo {@code ROLE_}.
 */
@Service
public class UsuariosService implements UserDetailsService {

    private UsuariosRepository usuariosRepository;

    public UsuariosService(UsuariosRepository usuariosRepository) {
        this.usuariosRepository = usuariosRepository;
    }

    /**
     * Carga un usuario por su nombre de usuario para el proceso de autenticación.
     *
     * @param username nombre de usuario ingresado en el formulario de login
     * @return {@link UserDetails} con credenciales y autoridades del usuario
     * @throws UsernameNotFoundException si el usuario no existe o está desactivado
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuarios usuario = usuariosRepository.findByNombreusuario(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        if (!usuario.getActivo()) {
            throw new UsernameNotFoundException("Usuario desactivado: " + username);
        }

        usuario.setUltimoAcceso(LocalDateTime.now());
        usuariosRepository.save(usuario);

        return new User(
                usuario.getNombreusuario(),
                usuario.getContrasena(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + usuario.getRol()))
        );
    }
}
