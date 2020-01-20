package ru.dimakron.multistacks_lib;

import android.view.View;

import androidx.annotation.AnimRes;
import androidx.annotation.StyleRes;
import androidx.core.util.Pair;

import java.util.List;


public class FragNavTransactionOptions {
    List<Pair<View, String>> sharedElements;
    int transition;
    @AnimRes
    int enterAnimation;
    @AnimRes
    int exitAnimation;
    @AnimRes
    int popEnterAnimation;
    @AnimRes
    int popExitAnimation;
    @StyleRes
    int transitionStyle;
    String breadCrumbTitle;
    String breadCrumbShortTitle;

    private FragNavTransactionOptions(Builder builder) {
        sharedElements = builder.sharedElements;
        transition = builder.transition;
        enterAnimation = builder.enterAnimation;
        exitAnimation = builder.exitAnimation;
        transitionStyle = builder.transitionStyle;
        popEnterAnimation = builder.popEnterAnimation;
        popExitAnimation = builder.popExitAnimation;
        breadCrumbTitle = builder.breadCrumbTitle;
        breadCrumbShortTitle = builder.breadCrumbShortTitle;
    }

    public static final class Builder {
        private List<Pair<View, String>> sharedElements;
        private int transition;
        private int enterAnimation;
        private int exitAnimation;
        private int transitionStyle;
        private int popEnterAnimation;
        private int popExitAnimation;
        private String breadCrumbTitle;
        private String breadCrumbShortTitle;

        private Builder() {
        }

        Builder customAnimations(@AnimRes int enterAnimation, @AnimRes int exitAnimation) {
            this.enterAnimation = enterAnimation;
            this.exitAnimation = exitAnimation;
            return this;
        }

        public Builder customAnimations(@AnimRes int enterAnimation, @AnimRes int exitAnimation, @AnimRes int popEnterAnimation, @AnimRes int popExitAnimation) {
            this.popEnterAnimation = popEnterAnimation;
            this.popExitAnimation = popExitAnimation;
            return customAnimations(enterAnimation, exitAnimation);
        }


        public FragNavTransactionOptions build() {
            return new FragNavTransactionOptions(this);
        }
    }
}

