package com.petcare.backend.model.converter;

import com.petcare.backend.model.enums.FriendRequestStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class FriendRequestStatusConverter implements AttributeConverter<FriendRequestStatus, String> {
    @Override
    public String convertToDatabaseColumn(FriendRequestStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public FriendRequestStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : FriendRequestStatus.fromValue(dbData);
    }
}
