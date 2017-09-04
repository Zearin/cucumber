---
menu:
- gherkin
source: https://github.com/cucumber/cucumber/wiki/Conjunction-Steps-(Antipattern)/
title: Conjunction Steps (Antipattern)
---

From the online Merriam-Webster dictionary:

> **con·junc·tion** :an uninflected linguistic form that joins together sentences, clauses, phrases, or words

Don't do this in Steps! It makes Steps too specialised and hard to reuse. Cucumber has built-in support for conjunctions (`And`, `But`) for a reason.

For example, don't do this:

```
# don't write conjunctions _into_ steps...
Given I have shades and a brand new Mustang
```

Instead, do this:

```
# ...write conjunctions _as_ steps!
Given I have shades
And I have a brand new Mustang
```

## When conjunction Steps are okay

Sometimes you may want to combine several Steps to make your Scenarios easier to read. This is certinaly possible to do (see [Calling Steps from Step Definitions](/implementations/ruby/calling-steps-from-step-definitions/)). 

But make your life easier, and strive to keep your called Steps atomic.
