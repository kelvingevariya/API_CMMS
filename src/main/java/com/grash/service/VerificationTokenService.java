package com.grash.service;

import com.grash.dto.AuthResponse;
import com.grash.model.OwnUser;
import com.grash.model.VerificationToken;
import com.grash.repository.VerificationTokenRepository;
import com.grash.security.JwtTokenProvider;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

@Service
@AllArgsConstructor
public class VerificationTokenService {

    private final UserService userService;
    private final VerificationTokenRepository verificationTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;


    public VerificationToken getVerificationTokenEntity(String token) {
        return verificationTokenRepository.findVerificationTokenEntityByToken(token);
    }

    public void deleteVerificationTokenEntity(OwnUser user) {
        ArrayList<VerificationToken> verificationToken = verificationTokenRepository.findAllVerificationTokenEntityByUser(user);
        verificationTokenRepository.deleteAll(verificationToken);
    }

    public void createVerificationToken(OwnUser user, String token) {
        VerificationToken newUserToken = new VerificationToken(token, user);
        verificationTokenRepository.save(newUserToken);
    }

    public AuthResponse confirmMail(String token) throws Exception {
        VerificationToken verificationToken = getVerificationTokenEntity(token);

        //invalid token
        if(verificationToken == null){
            String message = "Invalid activation link";
            throw new Exception(message);
        }

        //expired token
        OwnUser user = verificationToken.getUser();
        Calendar calendar = Calendar.getInstance();
        if((verificationToken.getExpiryDate().getTime() - calendar.getTime().getTime())<=0){
            String message = "Expired activation link!";
            throw new Exception(message);
        }

        //valid token
        userService.enableUser(user.getEmail());
        String message = "Account successfully activated !";

        return new AuthResponse(jwtTokenProvider.createToken(user.getEmail(), Arrays.asList(user.getRole().getRoleType())));
    }
}
