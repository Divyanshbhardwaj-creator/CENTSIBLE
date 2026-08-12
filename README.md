# CENTSIBL — AI-Powered Student Budget App

CENTSIBL is an Android budgeting app built for students. Log spending in plain language, track budgets by category, and get AI-powered financial insights.

CENTSIBLE is a portmanteau—a blend of the words "cent" (referring to money or coins) and "sensible". It means being wise, practical, or economical with money; cost-conscious or frugal.

## Features

- **Smart Expense Logging** — type naturally ("200 on food") and the app categorizes automatically
- **Can I Afford This?** — AI predictor that checks your remaining budget and gives a recommendation
- **Budget Tracking** — weekly limits per category with color-coded progress bars
- **Predictive Overspend Warnings** — forecasts which categories will bust their limit before the week ends
- **Recurring Charge Detection** — flags monthly subscriptions and calculates annual cost
- **Round-Up Savings Jar** — every purchase rounds up; spare change goes into savings automatically
- **Ideal Investment Goals** — savings jars with thermometer-style progress for goals (New Laptop, Trip, etc.)
- **Weekly Report** — swipeable story cards, donut chart, category breakdown, personalised tips
- **Budget Streak** — tracks consecutive weeks staying within budget

## Tech Stack

- **Language:** Java
- **Platform:** Android (minSdk 26 / Android 8.0+)
- **UI:** XML layouts, Material Components
- **Architecture:** MVVM with AndroidViewModel + LiveData
- **Persistence:** SharedPreferences
- **Chart:** MPAndroidChart

## How to Log Expenses

Use natural phrases:
- `200 on food` → Food category
- `spent 300 on entertainment` → Entertainment
- `ola 80` → Transport
- `flipkart 599` → Shopping
- `school fees 2000` → School
