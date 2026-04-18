package br.com.forum_hub.infra.seguranca;

import br.com.forum_hub.domain.autenticacao.TokenService;
import br.com.forum_hub.domain.usuario.Usuario;
import br.com.forum_hub.domain.usuario.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class FiltroTokenAcesso extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UsuarioRepository usuarioRepository;

    public FiltroTokenAcesso(TokenService tokenService, UsuarioRepository usuarioRepository) {
        this.tokenService = tokenService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = recuperarTokenRequisicao(request);

        if (token != null) {
            try {
                String email = tokenService.verificarToken(token);

                if (email != null && !email.isBlank()) {
                    Optional<Usuario> usuarioOptional = usuarioRepository.findByEmailIgnoreCase(email);

                    if (usuarioOptional.isPresent()) {
                        Usuario usuario = usuarioOptional.get();

                        Authentication authentication =
                                new UsernamePasswordAuthenticationToken(
                                        usuario,
                                        null,
                                        usuario.getAuthorities()
                                );

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String recuperarTokenRequisicao(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }

        String token = authorizationHeader.replace("Bearer ", "").trim();

        if (token.isBlank()) {
            return null;
        }

        return token;
    }
}