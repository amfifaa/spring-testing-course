package com.example.crashcourse.user;

import com.example.crashcourse.notification.WelcomeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final WelcomeService welcomeService;

    public UserService(UserRepository userRepository, WelcomeService welcomeService) {
        this.userRepository = userRepository;
        this.welcomeService = welcomeService;
    }

    // readOnly = true: a hint to the driver and database that this method only reads.
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    // @Transactional: everything in this method runs in ONE database transaction.
    // If any line throws a RuntimeException, the INSERT is rolled back automatically.
    // Careful: a *checked* exception still commits, unless you add rollbackFor = Exception.class.
    // Default propagation is REQUIRED: other @Transactional methods called from here join this same transaction.
    // (Works because Spring wraps this bean in a proxy that begins and commits/rolls back the transaction.)
    @Transactional
    public User create(CreateUserRequestDto request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new EmailAlreadyTakenException(request.email());
        }
        User saved = userRepository.save(new User(null, request.username(), request.email(), request.fullName()));
        welcomeService.welcome(saved.email());
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
    }
}
