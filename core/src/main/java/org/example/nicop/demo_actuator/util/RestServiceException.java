package org.example.nicop.demo_actuator.util;

import lombok.Data;
import org.springframework.http.HttpStatus;

/**
 * Une exception REST peut contenir plusieurs messages d'erreur.
 * Par cette approche, on peut ainsi prendre en compte des multi-erreurs, dans les formulaires par exemples.
 */
@Data
public class RestServiceException extends RuntimeException {

    RestErrorList errors = new RestErrorList();

    public RestServiceException( HttpStatus status, String message, String code, String detail) {
        super(message);
        addError(message, code, detail);
        errors.setStatus(status);
    }

    /**
     * ajoute une autre erreur à l'exception. Le cas d'usage est orienté formulaire
     * @param message, message de l'erreur
     * @param code, code fonctionnel de l'erreur
     * @param detail, detail de l'erreur
     */
    public void addError(String message, String code, String detail){
        errors.add(new ErrorMessage(message, code, detail));
    }

}