package net.j40climb.florafauna.common;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;

import java.util.List;
import java.util.Map;

public class AnimationUtils {

    /**
     * Creates a new AnimationDefinition that plays the given animation in reverse.
     * Useful for transition animations where A->B and B->A are mirror images.
     *
     * @param original the animation to reverse
     * @return a new AnimationDefinition that plays in reverse
     */
    public static AnimationDefinition reverse(AnimationDefinition original) {
        float length = original.lengthInSeconds();
        AnimationDefinition.Builder builder = AnimationDefinition.Builder.withLength(length);

        for (Map.Entry<String, List<AnimationChannel>> entry : original.boneAnimations().entrySet()) {
            String boneName = entry.getKey();
            for (AnimationChannel channel : entry.getValue()) {
                AnimationChannel reversedChannel = reverseChannel(channel, length);
                builder.addAnimation(boneName, reversedChannel);
            }
        }

        return builder.build();
    }

    private static AnimationChannel reverseChannel(AnimationChannel channel, float length) {
        Keyframe[] originalKeyframes = channel.keyframes();
        Keyframe[] reversedKeyframes = new Keyframe[originalKeyframes.length];

        for (int i = 0; i < originalKeyframes.length; i++) {
            Keyframe original = originalKeyframes[originalKeyframes.length - 1 - i];
            float newTimestamp = length - original.timestamp();
            // Swap pre and post targets for correct interpolation when reversed
            reversedKeyframes[i] = new Keyframe(
                    newTimestamp,
                    original.postTarget(),
                    original.preTarget(),
                    original.interpolation()
            );
        }

        return new AnimationChannel(channel.target(), reversedKeyframes);
    }
}
