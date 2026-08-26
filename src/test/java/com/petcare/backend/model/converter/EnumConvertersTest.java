package com.petcare.backend.model.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.petcare.backend.model.enums.CommentStatus;
import com.petcare.backend.model.enums.FriendRequestStatus;
import com.petcare.backend.model.enums.MediaType;
import com.petcare.backend.model.enums.PostPrivacy;
import com.petcare.backend.model.enums.PostStatus;
import com.petcare.backend.model.enums.ReactionType;
import com.petcare.backend.model.enums.SocialProvider;
import jakarta.persistence.AttributeConverter;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class EnumConvertersTest {
    private <E extends Enum<E>> void assertStandardConverter(
            AttributeConverter<E, String> converter, E[] values, Function<String, E> expected) {
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
        for (E value : values) {
            String databaseValue = converter.convertToDatabaseColumn(value);
            assertEquals(expected.apply(databaseValue), converter.convertToEntityAttribute(databaseValue));
            assertEquals(value, converter.convertToEntityAttribute(databaseValue.toUpperCase()));
        }
        assertThrows(IllegalArgumentException.class, () -> converter.convertToEntityAttribute(""));
        assertThrows(IllegalArgumentException.class, () -> converter.convertToEntityAttribute("not-a-value"));
    }

    @Test
    void postStatusConverter_MapsAllValuesNullWhitespaceCaseAndInvalid() {
        PostStatusConverter converter = new PostStatusConverter();
        assertStandardConverter(converter, PostStatus.values(), PostStatus::fromValue);
        assertEquals(PostStatus.PUBLISHED, converter.convertToEntityAttribute(" published "));
    }

    @Test
    void commentStatusConverter_MapsAllValuesNullWhitespaceCaseAndInvalid() {
        CommentStatusConverter converter = new CommentStatusConverter();
        assertStandardConverter(converter, CommentStatus.values(), CommentStatus::fromValue);
        assertEquals(CommentStatus.VISIBLE, converter.convertToEntityAttribute(" visible "));
    }

    @Test
    void reactionTypeConverter_MapsAllValuesNullWhitespaceCaseAndInvalid() {
        ReactionTypeConverter converter = new ReactionTypeConverter();
        assertStandardConverter(converter, ReactionType.values(), ReactionType::fromValue);
        assertEquals(ReactionType.LOVE, converter.convertToEntityAttribute(" love "));
    }

    @Test
    void friendRequestStatusConverter_MapsAllValuesNullWhitespaceCaseAndInvalid() {
        FriendRequestStatusConverter converter = new FriendRequestStatusConverter();
        assertStandardConverter(converter, FriendRequestStatus.values(), FriendRequestStatus::fromValue);
        assertEquals(FriendRequestStatus.CANCELLED, converter.convertToEntityAttribute(" cancelled "));
    }

    @Test
    void mediaTypeConverter_MapsAllValuesNullWhitespaceCaseAndInvalid() {
        MediaTypeConverter converter = new MediaTypeConverter();
        assertStandardConverter(converter, MediaType.values(), MediaType::fromValue);
        assertEquals(MediaType.DOCUMENT, converter.convertToEntityAttribute(" document "));
    }

    @Test
    void socialProviderConverter_MapsAllValuesNullCaseAndRejectsWhitespaceAndInvalid() {
        SocialProviderConverter converter = new SocialProviderConverter();
        assertStandardConverter(converter, SocialProvider.values(), SocialProvider::fromValue);
        assertThrows(IllegalArgumentException.class, () -> converter.convertToEntityAttribute(" google "));
    }

    @Test
    void postPrivacyConverter_MapsAllValuesNullAliasWhitespaceCaseAndInvalid() {
        PostPrivacyConverter converter = new PostPrivacyConverter();
        assertStandardConverter(converter, PostPrivacy.values(), PostPrivacy::fromValue);
        assertEquals(PostPrivacy.FRIENDS, converter.convertToEntityAttribute(" followers "));
        assertEquals(PostPrivacy.PRIVATE, converter.convertToEntityAttribute(" private "));
    }
}
