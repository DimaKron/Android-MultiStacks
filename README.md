Android MultiStacks      
===================  
<img src="https://github.com/DimaKron/Android-MultiStacks/blob/master/sample.gif" width="50%">

implementation 'com.github.DimaKron:Android-MultiStacks:1.0'

## Getting started

### Dependency

Add following line to root `build.gradle`

```groovy
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

and line to modul level `build.gradle`

```groovy
dependencies {
    implementation 'com.github.DimaKron:Android-MultiStacks:1.1'
}
```

### Usage

#### Initialization
Initialize component with Builder in your main navigation activity

```kotlin
multiStacks = MultiStacks.Builder(supportFragmentManager, R.id.containerLayout)
            .setState(savedInstanceState)
            .setRootFragmentInitializers(fragmentInitializers)
            .setSelectedTabIndex(0)
            .setTabHistoryEnabled(true)
            .setTransactionListener(this)
            .build()
```

#### Saving state

Override method of navigation activity

```kotlin
override fun onSaveInstanceState(outState: Bundle) {
        multiStacks.saveInstanceState(outState)
        super.onSaveInstanceState(outState)
}
```

#### Back action

Call method to perform back action (close fragment or back to other tab)

```kotlin
...
 multiStacks.onBackPressed()
...
```

#### Manage

Use methods following methods for navigation managent

- `setSelectedTabIndex(Int)` change tab
- `push(Fragment)` add new fragment to current tab
- `replace(Fragment)` replace current with new fragment
- `popFragments(Int)` remove X last fragments of current stack
- `clearStack()` remove all fragments of current stack (except first)
- `clearStack()` remove all fragments of current stack (except first)

### Other features

#### Unique fragment in stack

Your fragment should implements `getIdentifierInStack()` of `IMultiStackFragment`interface

#### Built-in tabs history

Enable tabs history with `MultiStack.Builder.setTabHistoryEnabled(true)`
