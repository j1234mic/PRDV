package com.prdv.profile;

import com.prdv.profile.domain.model.RppsNumber;
import com.prdv.shared.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Les Value Objects auto-validants rendent les etats invalides IRREPRESENTABLES. */
class ValueObjectsTest {

    @Test
    void valid_rpps_accepts_correct_key() {
        String number = RppsNumber.completeWithKey("100003795"); // 9 chiffres + cle calculee
        assertEquals(11, number.length());
        assertEquals(number, new RppsNumber(number).value());
    }

    @Test
    void wrong_rpps_key_is_rejected() {
        String number = RppsNumber.completeWithKey("100003795");
        String tampered = number.substring(0, 10) + ((Integer.parseInt(number.substring(10)) + 1) % 10);
        assertThrows(ValidationException.class, () -> new RppsNumber(tampered));
    }

    @Test
    void malformed_rpps_is_rejected() {
        assertThrows(ValidationException.class, () -> new RppsNumber("12345"));      // trop court
        assertThrows(ValidationException.class, () -> new RppsNumber("1000037954A")); // lettre
    }
}
