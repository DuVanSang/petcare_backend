package com.petcare.backend.dto.user.request;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {
    @Size(max = 100, message = "Họ tên không được vượt quá 100 ký tự")
    private String fullName;

    @Pattern(
            regexp = "^$|^[a-zA-Z0-9._]{3,30}$",
            message = "Username chỉ gồm chữ, số, dấu chấm, gạch dưới và dài 3-30 ký tự"
    )
    private String username;

    @Size(max = 150, message = "Bio không được vượt quá 150 ký tự")
    private String bio;

    @Past(message = "Ngày sinh phải là ngày trong quá khứ")
    private LocalDate dateOfBirth;

    @Size(max = 150, message = "Địa điểm không được vượt quá 150 ký tự")
    private String location;

    @Pattern(regexp = "^$|\\d{10}", message = "Số điện thoại phải gồm 10 chữ số")
    private String phoneNumber;

    @Size(max = 1024, message = "Đường dẫn ảnh đại diện không được vượt quá 1024 ký tự")
    private String avatarUrl;

    @Size(max = 1024, message = "Đường dẫn ảnh bìa không được vượt quá 1024 ký tự")
    private String coverImageUrl;
}
