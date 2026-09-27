package io.github.samuelvno.paymentreconciliation.auth;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.samuelvno.paymentreconciliation.shared.error.BusinessConflictException;
import io.github.samuelvno.paymentreconciliation.store.StoreService;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final StoreService storeService;
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService,
                       StoreService storeService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.storeService = storeService;
        this.dummyHash = passwordEncoder.encode("timing-attack-dummy-password");
    }

    public TokenResponse login(LoginRequest request) {
        AppUser user = userRepository.findByEmailIgnoreCase(request.email()).orElse(null);
        // Mesmo sem usuário, roda o BCrypt: o tempo de resposta não revela quais e-mails existem.
        String hash = user == null ? dummyHash : user.getPasswordHash();
        boolean matches = passwordEncoder.matches(request.password(), hash);
        if (user == null || !matches) {
            // Mensagem única para e-mail inexistente e senha errada (evita enumeração de usuários)
            throw new BadCredentialsException("Invalid email or password");
        }
        return tokenService.issue(user);
    }

    @Transactional
    public UserResponse createStoreUser(CreateUserRequest request) {
        storeService.getStore(request.storeId());
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessConflictException("A user with email %s already exists".formatted(request.email()));
        }
        AppUser user = AppUser.storeUser(request.email().toLowerCase(), passwordEncoder.encode(request.password()),
                request.storeId());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public boolean createAdminIfMissing(String email, String rawPassword) {
        if (userRepository.existsByRole(Role.ADMIN) || userRepository.existsByEmailIgnoreCase(email)) {
            return false;
        }
        userRepository.save(AppUser.admin(email.toLowerCase(), passwordEncoder.encode(rawPassword)));
        return true;
    }
}
