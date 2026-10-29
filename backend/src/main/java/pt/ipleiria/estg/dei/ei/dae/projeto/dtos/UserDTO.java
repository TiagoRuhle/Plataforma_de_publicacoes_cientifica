package pt.ipleiria.estg.dei.ei.dae.projeto.dtos;

import org.hibernate.Hibernate;
import pt.ipleiria.estg.dei.ei.dae.projeto.entities.User;

import java.util.List;
import java.util.stream.Collectors;

public class UserDTO {
    private String username;
    private String name;
    private String email;
    private String role;
    private Boolean active;

    public UserDTO() {
    }

    public UserDTO(String username, String name, String email, String role, Boolean active) {
        this.username = username;
        this.name = name;
        this.email = email;
        this.role = role;
        this.active = active;
    }

    public static UserDTO from(User user) {
        return new UserDTO(
                user.getUsername(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isActive()
        );
    }

    public static List<UserDTO> from(List<User> users) {
        return users.stream().map(UserDTO::from).collect(Collectors.toList());
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean isActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}