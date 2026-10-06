# Compliance Scoring Specification

## Purpose

Defines how the app computes the inspection compliance score from checklist answers, derives the strict 85% reinspection rule with its 24h window, and preserves the Critical Stop contract. All score rules are pure functions in the ViewModel layer so they are unit-testable.

## Requirements

### Requirement: Weighted Score Computation

The system MUST compute the compliance score as `(obtainedWeights * 100) / totalWeights` using integer (floor) division. Each answered item contributes its criticality weight (HIGH=5, MEDIUM=3, LOW=1); an item counts as answered ONLY when `answers[id] == true`. Unanswered items MUST count as non-compliant (zero weight).

#### Scenario: Weighted floor score

- GIVEN a checklist with total weight 13 and obtained weight 9
- WHEN the score is computed
- THEN it is 69, floored (never rounded to 69.2)

#### Scenario: Only true answers count

- GIVEN an item answered false and an item absent from answers
- WHEN the score is computed
- THEN both contribute zero weight

#### Scenario: All compliant

- GIVEN every item has `answers[id] == true`
- WHEN the score is computed
- THEN it is 100

#### Scenario: No answers

- GIVEN no item is answered
- WHEN the score is computed
- THEN it is 0

### Requirement: Empty Checklist Score

The system MUST return 100 when the score is computed over a checklist with no items.

#### Scenario: Empty checklist

- GIVEN a checklist with zero items
- WHEN the score is computed
- THEN it is 100

### Requirement: Reinspection Threshold (strict)

The system MUST mark an inspection reinspection-due iff its score is strictly below 85; a score of exactly 85 MUST NOT be due.

#### Scenario: Score 84 is due

- GIVEN a score of 84
- WHEN the threshold rule is evaluated
- THEN the inspection is reinspection-due

#### Scenario: Score 85 is not due

- GIVEN a score of 85
- WHEN the threshold rule is evaluated
- THEN the inspection is NOT reinspection-due

### Requirement: Reinspection Window (24h rule)

The system MUST schedule the reinspection of a due inspection exactly 24 hours after the inspection is confirmed.

#### Scenario: Due inspection gets 24h window

- GIVEN a reinspection-due inspection confirmed at time T
- WHEN the 24h rule is applied
- THEN the reinspection is scheduled at T plus 24 hours

### Requirement: Critical Stop Contract (preserved)

The system MUST trigger Critical Stop when any HIGH-criticality item is non-compliant. The score and threshold logic MUST NOT change this behavior; Critical Stop remains an independent gate.

#### Scenario: HIGH non-compliance triggers stop

- GIVEN an unanswered HIGH-criticality item, even when the score is 85 or higher
- WHEN the inspection state is evaluated
- THEN Critical Stop is triggered

#### Scenario: No HIGH non-compliance

- GIVEN all HIGH-criticality items answered true
- WHEN the inspection state is evaluated
- THEN Critical Stop is not triggered