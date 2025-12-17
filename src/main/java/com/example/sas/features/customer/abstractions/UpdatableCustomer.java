package com.example.sas.features.customer.abstractions;

import java.time.LocalDate;

public interface UpdatableCustomer {
    String getSsn();
    String getFirstName();
    String getLastName();
    LocalDate getBirthday();
    String getEmail();
    String getPhone();

    void setFirstName(String firstName);
    void setLastName(String lastName);
    void setBirthday(LocalDate birthday);
    void setEmail(String email);
    void setPhone(String phone);
}
