package com.moneyminder.app;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.moneyminder.app.model.BudgetStatus;
import com.moneyminder.app.model.Category;
import com.moneyminder.app.model.RecurringCharge;
import com.moneyminder.app.model.SavingsGoal;
import com.moneyminder.app.model.TipCard;
import com.moneyminder.app.model.Transaction;
import com.moneyminder.app.model.WeeklyReport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MoneyMinderViewModel extends AndroidViewModel {

    private static final Map<Category, List<String>> KEYWORDS = new HashMap<>();
    static {
        KEYWORDS.put(Category.FOOD, Arrays.asList(
                "food","lunch","dinner","breakfast","snack","pizza","burger",
                "coffee","boba","bubble tea","restaurant","mcdonald","chipotle",
                "starbucks","taco","sushi","fries","doordash","grubhub","zomato","swiggy",
                "eating","cafe","dhaba","chai","biryani","dosa","samosa","juice",
                "thali","paratha","roti","sabzi","dal","idli","vada","noodles","pasta"));
        KEYWORDS.put(Category.ENTERTAINMENT, Arrays.asList(
                "entertainment","movie","movies","cinema","netflix","game","games","gaming",
                "concert","ticket","tickets","theater","steam","arcade","bowling","pvr","inox",
                "fun","outing","show","series","web series","ott","amusement","park","ride",
                "youtube","recharge","streaming","netflix","hotstar","prime video"));
        KEYWORDS.put(Category.TRANSPORT, Arrays.asList(
                "transport","uber","lyft","ola","bus","petrol","diesel","train","taxi","subway",
                "parking","fare","auto","rickshaw","metro","travel","commute","cab","rapido",
                "flight","ticket","toll","fuel","pump","scooter","bike","cycle"));
        KEYWORDS.put(Category.SHOPPING, Arrays.asList(
                "shopping","clothes","clothing","shoes","footwear","amazon","flipkart","mall",
                "shirt","sneakers","nike","adidas","jeans","hoodie","makeup","sephora","myntra",
                "market","bazaar","store","purchase","dress","kurta","saree","watch","accessory",
                "nykaa","meesho","ajio","decathlon","lifestyle","westside"));
        KEYWORDS.put(Category.SCHOOL, Arrays.asList(
                "school","college","books","supplies","notebook","tuition","textbook",
                "binder","printing","stationery","fees","exam","study","course","class",
                "coaching","pencil","eraser","geometry","assignment","project","lab"));
        KEYWORDS.put(Category.SUBSCRIPTIONS, Arrays.asList(
                "subscription","spotify","hulu","disney+","apple music","youtube premium",
                "gym","membership","netflix","prime","hotstar","zee5","sonyliv","jiocinema",
                "gaana","wynk","annual plan","monthly plan","renewal"));
    }

    private static final Map<Category, Double> DEFAULT_BUDGETS = new HashMap<>();
    static {
        DEFAULT_BUDGETS.put(Category.FOOD,          500.0);
        DEFAULT_BUDGETS.put(Category.ENTERTAINMENT, 300.0);
        DEFAULT_BUDGETS.put(Category.TRANSPORT,     200.0);
        DEFAULT_BUDGETS.put(Category.SHOPPING,      400.0);
        DEFAULT_BUDGETS.put(Category.SCHOOL,        250.0);
        DEFAULT_BUDGETS.put(Category.SUBSCRIPTIONS, 150.0);
        DEFAULT_BUDGETS.put(Category.OTHER,         150.0);
    }

    private static final double DEFAULT_GENERAL_GOAL_TARGET = 1000.0;

    private static final String PREFS       = "centsibl_prefs";
    private static final String KEY_TXNS    = "transactions";
    private static final String KEY_BUDGETS = "budgets";
    private static final String KEY_JAR     = "roundup_jar";
    private static final String KEY_STREAK  = "streak_weeks";
    private static final String KEY_WEEK    = "week_start";
    private static final String KEY_GOALS   = "savings_goals";

    private final SharedPreferences prefs;

    public final MutableLiveData<List<Transaction>> transactions = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<Map<Category, Double>> budgets  = new MutableLiveData<>(new HashMap<>(DEFAULT_BUDGETS));
    public final MutableLiveData<Double>  roundUpJar             = new MutableLiveData<>(0.0);
    public final MutableLiveData<Integer> streakWeeks            = new MutableLiveData<>(0);
    public final MutableLiveData<Long>    weekStart              = new MutableLiveData<>(currentMonday());
    public final MutableLiveData<List<SavingsGoal>> savingsGoals = new MutableLiveData<>(new ArrayList<>());
    
    public final MutableLiveData<String>  lastEvent              = new MutableLiveData<>(null);

    public MoneyMinderViewModel(Application app) {
        super(app);
        prefs = app.getSharedPreferences(PREFS, Application.MODE_PRIVATE);
        load();
    }

    public Category categorizeExpense(String text) {
        String lower = text.toLowerCase();
        for (Category cat : Category.values()) {
            List<String> kws = KEYWORDS.get(cat);
            if (kws == null) continue;
            for (String kw : kws) {
                
                if (kw.contains(" ")) {
                    if (lower.contains(kw)) return cat;
                } else {
                    
                    if (lower.matches(".*\\b" + java.util.regex.Pattern.quote(kw) + "\\b.*")) return cat;
                }
            }
        }
        return Category.OTHER;
    }

    public double parseAmount(String text) {
        Matcher m = Pattern.compile("\\d+(\\.\\d{1,2})?").matcher(text);
        if (m.find()) {
            try { return Double.parseDouble(m.group()); } catch (NumberFormatException ignored) {}
        }
        return -1;
    }

    private String extractDescription(String text, String amountToken) {
        String cleaned = text
                .replaceAll("(?i)\\bspent\\b", "")
                .replaceAll("(?i)\\bbought\\b", "")
                .replaceAll("(?i)\\bpaid\\b",   "")
                .replaceAll("(?i)\\bfor\\b",    "")
                .replaceAll("(?i)\\bon\\b",     "")
                .replace(amountToken, "")
                .replace("$", "")
                .replaceAll("\\s+", " ")
                .trim();
        if (cleaned.isEmpty()) cleaned = "purchase";
        return Character.toUpperCase(cleaned.charAt(0)) + cleaned.substring(1);
    }

    public BudgetStatus checkBudgetLimit(Category category) {
        List<Transaction> txns = transactions.getValue();
        long ws = weekStart.getValue() != null ? weekStart.getValue() : 0L;
        double spent = 0;
        if (txns != null) {
            for (Transaction t : txns) {
                if (t.timestamp >= ws && t.category == category) spent += t.amount;
            }
        }
        Map<Category, Double> b = budgets.getValue();
        double limit = (b != null && b.containsKey(category)) ? b.get(category) : 0.0;
        return new BudgetStatus(category, spent, limit);
    }

    public List<BudgetStatus> allBudgetStatuses() {
        List<BudgetStatus> list = new ArrayList<>();
        for (Category c : Category.values()) list.add(checkBudgetLimit(c));
        return list;
    }

    public void logExpense(String rawText) {
        double amount = parseAmount(rawText);
        if (amount < 0) {
            lastEvent.setValue("no_amount");
            return;
        }

        Matcher m = Pattern.compile("\\d+(\\.\\d{1,2})?").matcher(rawText);
        String amountToken = m.find() ? m.group() : String.valueOf((int) amount);

        Category category   = categorizeExpense(rawText);
        String   description = extractDescription(rawText, amountToken);

        Transaction txn = new Transaction(
                System.currentTimeMillis() + (int)(Math.random() * 1000),
                amount, category, description, System.currentTimeMillis()
        );
        List<Transaction> current = new ArrayList<>();
        if (transactions.getValue() != null) current.addAll(transactions.getValue());
        current.add(0, txn);
        transactions.setValue(current);

        double roundUp = Math.ceil(amount) - amount;
        double jar = roundUpJar.getValue() != null ? roundUpJar.getValue() : 0.0;
        roundUpJar.setValue(jar + roundUp);

        persist();

        BudgetStatus status = checkBudgetLimit(category);
        switch (status.state) {
            case OVER:    lastEvent.setValue("over:" + category.label);    break;
            case WARNING: lastEvent.setValue("warning:" + category.label); break;
            default:      lastEvent.setValue("ok:" + category.label);      break;
        }
    }

    public void consumeEvent() { lastEvent.setValue(null); }

    public void updateBudget(Category category, double newLimit) {
        Map<Category, Double> b = new HashMap<>();
        if (budgets.getValue() != null) b.putAll(budgets.getValue());
        b.put(category, newLimit);
        budgets.setValue(b);
        persist();
    }

    public void updateAllBudgets(Map<Category, Double> newBudgets) {
        Map<Category, Double> b = new HashMap<>(DEFAULT_BUDGETS);
        b.putAll(newBudgets);
        budgets.setValue(b);
        persist();
    }

    public void addSavingsGoal(String name, double target) {
        List<SavingsGoal> current = new ArrayList<>();
        if (savingsGoals.getValue() != null) current.addAll(savingsGoals.getValue());
        current.add(new SavingsGoal(System.currentTimeMillis() + (int) (Math.random() * 1000),
                name, target, 0.0));
        savingsGoals.setValue(current);
        persist();
    }

    public void contributeToGoal(long goalId, double amount) {
        List<SavingsGoal> current = savingsGoals.getValue();
        if (current == null) return;
        for (SavingsGoal g : current) {
            if (g.id == goalId) { g.saved += amount; break; }
        }
        savingsGoals.setValue(current);
        persist();
    }

    public void deleteGoal(long goalId) {
        List<SavingsGoal> current = savingsGoals.getValue();
        if (current == null) return;
        List<SavingsGoal> next = new ArrayList<>();
        for (SavingsGoal g : current) if (g.id != goalId) next.add(g);
        savingsGoals.setValue(next);
        persist();
    }

    public void autoDistributeRoundUps() {
        double jar = roundUpJar.getValue() != null ? roundUpJar.getValue() : 0.0;
        if (jar <= 0) return;

        List<SavingsGoal> current = new ArrayList<>();
        if (savingsGoals.getValue() != null) current.addAll(savingsGoals.getValue());
        if (current.isEmpty()) {
            
            current.add(new SavingsGoal(System.currentTimeMillis(), "General Savings", DEFAULT_GENERAL_GOAL_TARGET, 0.0));
        }

        double share = jar / current.size();
        for (SavingsGoal g : current) g.saved += share;

        savingsGoals.setValue(current);
        roundUpJar.setValue(0.0);
        persist();
    }

    public void logIncome(double amount, Long goalId) {
        if (amount <= 0) return;
        double save = Math.round(amount * 0.20 * 100) / 100.0;
        double spendable = Math.round((amount - save) * 100) / 100.0;

        List<SavingsGoal> current = new ArrayList<>();
        if (savingsGoals.getValue() != null) current.addAll(savingsGoals.getValue());

        SavingsGoal target = null;
        if (goalId != null) {
            for (SavingsGoal g : current) if (g.id == goalId) { target = g; break; }
        }
        if (target == null) {
            if (current.isEmpty()) {
                target = new SavingsGoal(System.currentTimeMillis(), "General Savings", DEFAULT_GENERAL_GOAL_TARGET, 0.0);
                current.add(target);
            } else {
                target = current.get(0);
            }
        }
        target.saved += save;
        savingsGoals.setValue(current);
        persist();

        lastEvent.setValue("income:" + save + ":" + spendable);
    }

    public List<RecurringCharge> detectRecurringCharges() {
        List<Transaction> txns = transactions.getValue();
        List<RecurringCharge> results = new ArrayList<>();
        if (txns == null || txns.isEmpty()) return results;

        Map<String, List<Transaction>> groups = new LinkedHashMap<>();
        for (Transaction t : txns) {
            String key = t.category.name() + "|" + t.description.trim().toLowerCase();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
        }

        for (List<Transaction> group : groups.values()) {
            if (group.size() < 2) continue;
            double avg = 0;
            for (Transaction t : group) avg += t.amount;
            avg /= group.size();

            boolean consistent = true;
            for (Transaction t : group) {
                if (Math.abs(t.amount - avg) > avg * 0.10 + 0.01) { consistent = false; break; }
            }
            if (!consistent) continue;

            double annual = avg * 12;
            results.add(new RecurringCharge(group.get(0).description, group.get(0).category, avg, annual, group.size()));
        }

        results.sort(Comparator.comparingDouble((RecurringCharge r) -> r.annualCost).reversed());
        return results;
    }

    public String checkAffordability(String query) {
        double price = parseAmount(query);
        if (price <= 0) {
            return "Tell me the price too — e.g. \"Can I afford a ₹1200 jacket today?\"";
        }

        double totalSpent = 0, totalBudget = 0;
        for (Category c : Category.values()) {
            BudgetStatus s = checkBudgetLimit(c);
            totalSpent  += s.spent;
            totalBudget += s.limit;
        }
        double remaining = totalBudget - totalSpent;

        long ws = weekStart.getValue() != null ? weekStart.getValue() : currentMonday();
        long daysElapsed = Math.max(1, (System.currentTimeMillis() - ws) / 86400000L);
        long daysLeft = Math.max(1, 7 - daysElapsed);

        double afterPurchase = remaining - price;
        double perDayNow   = remaining / daysLeft;
        double perDayAfter = afterPurchase / daysLeft;

        StringBuilder msg = new StringBuilder();
        msg.append("You have ₹").append(money(remaining)).append(" left, but your weekly budget resets in ")
           .append(daysLeft).append(daysLeft == 1 ? " day.\n" : " days.\n");

        if (afterPurchase < 0) {
            msg.append("Buying this puts you ₹").append(money(-afterPurchase))
               .append(" over your remaining budget.\n");
            msg.append("Recommendation: Skip it, or wait until next week! 🚫");
        } else {
            msg.append("Buying this leaves you with only ₹").append(money(perDayAfter))
               .append("/day for the rest of the week.\n");
            if (perDayAfter < perDayNow * 0.4) {
                msg.append("Recommendation: Wait until next week! ⏳");
            } else if (perDayAfter < perDayNow * 0.7) {
                msg.append("Recommendation: You can — but spend carefully the rest of the week. 👀");
            } else {
                msg.append("Recommendation: Go for it, you're comfortably within budget! ✅");
            }
        }
        return msg.toString();
    }

    private static String money(double v) {
        return String.format("%.0f", v);
    }

    public String forecastBustDay(Category category) {
        BudgetStatus s = checkBudgetLimit(category);
        if (s.state != BudgetStatus.State.OK || s.limit <= 0 || s.spent <= 0) return null;

        long ws = weekStart.getValue() != null ? weekStart.getValue() : currentMonday();
        long daysElapsed = Math.max(1, (System.currentTimeMillis() - ws) / 86400000L);
        double dailyRate = s.spent / daysElapsed;
        if (dailyRate <= 0) return null;

        double remaining = s.limit - s.spent;
        long daysUntilBust = (long) Math.ceil(remaining / dailyRate);
        if (daysElapsed + daysUntilBust > 7) return null;

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ws);
        cal.add(Calendar.DAY_OF_MONTH, (int) (daysElapsed + daysUntilBust));
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault());
        return sdf.format(cal.getTime());
    }

    public void resetAllData() {
        transactions.setValue(new java.util.ArrayList<>());
        budgets.setValue(new java.util.HashMap<>(DEFAULT_BUDGETS));
        roundUpJar.setValue(0.0);
        streakWeeks.setValue(0);
        weekStart.setValue(currentMonday());
        prefs.edit().clear().apply();
    }

    public void startNewWeek() {
        boolean brokeStreak = false;
        for (BudgetStatus s : allBudgetStatuses()) {
            if (s.state == BudgetStatus.State.OVER) { brokeStreak = true; break; }
        }
        int streak = streakWeeks.getValue() != null ? streakWeeks.getValue() : 0;
        streakWeeks.setValue(brokeStreak ? 0 : streak + 1);
        long ws = weekStart.getValue() != null ? weekStart.getValue() : currentMonday();
        weekStart.setValue(ws + 7L * 24 * 60 * 60 * 1000);
        persist();
    }

    public WeeklyReport generateReport() {
        List<Transaction> txns = transactions.getValue();
        long ws = weekStart.getValue() != null ? weekStart.getValue() : 0L;
        Map<Category, Double> byCategory = new HashMap<>();
        for (Category c : Category.values()) byCategory.put(c, 0.0);
        double total = 0;
        if (txns != null) {
            for (Transaction t : txns) {
                if (t.timestamp >= ws) {
                    byCategory.put(t.category, byCategory.get(t.category) + t.amount);
                    total += t.amount;
                }
            }
        }

        Map<Category, Double> b = budgets.getValue() != null ? budgets.getValue() : DEFAULT_BUDGETS;
        double totalBudget = 0;
        for (double v : b.values()) totalBudget += v;

        Category topCategory = null;
        double topAmt = 0;
        for (Map.Entry<Category, Double> e : byCategory.entrySet()) {
            if (e.getValue() > topAmt) { topAmt = e.getValue(); topCategory = e.getKey(); }
        }

        List<String> tips = new ArrayList<>();
        List<TipCard> tipCards = new ArrayList<>();

        for (BudgetStatus s : allBudgetStatuses()) {
            if (s.state == BudgetStatus.State.OVER) {
                int cap = Math.max(5, (int) (s.limit / 4));
                double over = s.spent - s.limit;
                tips.add("⚠️ " + s.category.label + " is ₹" + String.format("%.2f", over)
                        + " over budget. Try a per-purchase cap of about ₹" + cap + " next week.");
                tipCards.add(new TipCard(TipCard.Urgency.RED, s.category.emoji,
                        s.category.label + " Insight",
                        "You've gone ₹" + String.format("%.0f", over) + " over your " + s.category.label.toLowerCase()
                                + " limit this week. A per-purchase cap of about ₹" + cap + " keeps you in check.",
                        "🔴 Over by ₹" + String.format("%.0f", over),
                        "Tighten cap", "cap:" + s.category.name()));
            } else if (s.state == BudgetStatus.State.WARNING) {
                double left = s.limit - s.spent;
                tips.add("👀 " + s.category.label + " is at " + (int) (s.pct * 100)
                        + "% of its budget — ₹" + String.format("%.2f", left) + " left.");
                tipCards.add(new TipCard(TipCard.Urgency.YELLOW, s.category.emoji,
                        s.category.label + " Insight",
                        "You've hit " + (int) (s.pct * 100) + "% of your " + s.category.label.toLowerCase()
                                + " budget. Only ₹" + String.format("%.0f", left) + " left this week.",
                        "🟡 ₹" + String.format("%.0f", left) + " left",
                        "Review budget", "open_edit_budgets"));
            }
        }

        long daysElapsed = Math.max(1, (System.currentTimeMillis() - ws) / 86400000L);
        for (BudgetStatus s : allBudgetStatuses()) {
            if (s.state == BudgetStatus.State.OK && s.limit > 0 && s.spent > 0) {
                double dailyRate = s.spent / daysElapsed;
                double projected = dailyRate * 7;
                if (projected > s.limit) {
                    tips.add("📈 At your current pace, " + s.category.label + " is on track to hit ₹"
                            + String.format("%.2f", projected) + " by week's end — above your ₹"
                            + String.format("%.0f", s.limit) + " limit.");
                    tipCards.add(new TipCard(TipCard.Urgency.YELLOW, "📈",
                            s.category.label + " Forecast",
                            "At this rate, you'll hit ₹" + String.format("%.0f", projected) + " in "
                                    + s.category.label.toLowerCase() + " by week's end — above your ₹"
                                    + String.format("%.0f", s.limit) + " limit.",
                            null, "Adjust budget", "open_edit_budgets"));
                }
            }
        }

        if (topCategory != null && total > 0) {
            int pct = (int) (byCategory.get(topCategory) / total * 100);
            tips.add("📊 " + topCategory.label + " was your biggest category at ₹"
                    + String.format("%.2f", byCategory.get(topCategory)) + "  (" + pct + "% of total).");
        }

        boolean anyOver = false;
        for (BudgetStatus s : allBudgetStatuses()) if (s.state == BudgetStatus.State.OVER) { anyOver = true; break; }
        if (!anyOver && total <= totalBudget) {
            double savedAmt = totalBudget - total;
            tips.add("✅ You're ₹" + String.format("%.2f", savedAmt)
                    + " under your total weekly budget. Move it to savings before it disappears!");
            tipCards.add(new TipCard(TipCard.Urgency.GREEN, "✅", "Great job!",
                    "You're ₹" + String.format("%.0f", savedAmt) + " under your total weekly budget. "
                            + "Move it to savings before it disappears!",
                    "⚡ Potential Savings: ₹" + String.format("%.0f", savedAmt),
                    "Save it", "open_jars"));
        }

        if (txns == null || txns.isEmpty()) {
            tips.add("💡 Nothing logged yet this week. Try it right after your next purchase — it takes 5 seconds.");
        }

        List<RecurringCharge> recurring = detectRecurringCharges();
        if (!recurring.isEmpty()) {
            double annualTotal = 0;
            for (RecurringCharge r : recurring) annualTotal += r.annualCost;
            tips.add("💡 " + recurring.size() + " recurring charge" + (recurring.size() == 1 ? "" : "s")
                    + " detected, costing about ₹" + String.format("%.0f", annualTotal) + "/year combined.");
            tipCards.add(new TipCard(TipCard.Urgency.PURPLE, "🔁", "Subscription Check",
                    recurring.size() + " recurring charge" + (recurring.size() == 1 ? "" : "s")
                            + " detected, costing about ₹" + String.format("%.0f", annualTotal) + "/year combined.",
                    "⚡ Potential Savings: ₹" + String.format("%.0f", annualTotal / 12) + "/mo",
                    null, null));
        }

        double jarNow = roundUpJar.getValue() != null ? roundUpJar.getValue() : 0.0;
        if (jarNow > 0) {
            tips.add("💡 Your round-up jar has ₹" + String.format("%.2f", jarNow)
                    + " sitting in it — move it into a savings goal on the Jars screen.");
            tipCards.add(new TipCard(TipCard.Urgency.PURPLE, "🪙", "Round-Up Jar",
                    "Your round-up jar has ₹" + String.format("%.2f", jarNow)
                            + " sitting in it, doing nothing. Move it into a goal so it starts working for you.",
                    "⚡ ₹" + String.format("%.2f", jarNow) + " ready",
                    "Distribute now", "distribute_jar"));
        }

        int streak = streakWeeks.getValue() != null ? streakWeeks.getValue() : 0;
        return new WeeklyReport(byCategory, total, totalBudget, topCategory, tips, tipCards, generateStoryCards(), streak);
    }

    public List<TipCard> generateStoryCards() {
        List<TipCard> cards = new ArrayList<>();
        List<Transaction> txns = transactions.getValue();
        long ws = weekStart.getValue() != null ? weekStart.getValue() : currentMonday();
        if (txns == null || txns.isEmpty()) return cards;

        java.util.Set<Integer> weekendDaySet = new java.util.HashSet<>();
        java.util.Set<Integer> weekdayDaySet = new java.util.HashSet<>();
        double weekendTotal = 0, weekdayTotal = 0;
        Calendar cal = Calendar.getInstance();
        for (Transaction t : txns) {
            if (t.timestamp < ws) continue;
            cal.setTimeInMillis(t.timestamp);
            int dow = cal.get(Calendar.DAY_OF_WEEK);
            int dayOfYear = cal.get(Calendar.DAY_OF_YEAR);
            boolean isWeekend = (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY);
            if (isWeekend) { weekendTotal += t.amount; weekendDaySet.add(dayOfYear); }
            else           { weekdayTotal += t.amount; weekdayDaySet.add(dayOfYear); }
        }
        int weekendDaysSeen = Math.max(1, weekendDaySet.size());
        int weekdayDaysSeen = Math.max(1, weekdayDaySet.size());
        double weekendAvg = weekendTotal / weekendDaysSeen;
        double weekdayAvg = weekdayTotal / weekdayDaysSeen;
        if (weekendTotal > 0 && weekdayAvg > 0) {
            int pctMore = (int) Math.round((weekendAvg - weekdayAvg) / weekdayAvg * 100);
            if (pctMore >= 15) {
                cards.add(new TipCard(TipCard.Urgency.YELLOW, "📅", "Weekend Spending Warning",
                        "You spend " + pctMore + "% more on weekends than on weekdays.",
                        null, null, null));
            }
        }

        double cabTotal = 0; int cabCount = 0;
        String[] cabKeywords = {"uber", "ola", "cab", "taxi", "rapido"};
        for (Transaction t : txns) {
            if (t.timestamp < ws || t.category != Category.TRANSPORT) continue;
            String desc = t.description.toLowerCase();
            for (String kw : cabKeywords) {
                if (desc.contains(kw)) { cabTotal += t.amount; cabCount++; break; }
            }
        }
        if (cabCount >= 2) {
            double avgCabFare = cabTotal / cabCount;
            double busFare = 30.0;
            int swapRides = cabCount / 2;
            double weeklySavings = Math.max(0, (avgCabFare - busFare) * swapRides);
            if (weeklySavings > 0) {
                cards.add(new TipCard(TipCard.Urgency.GREEN, "🚌", "Smart Swap",
                        "Swapping " + swapRides + " cab ride" + (swapRides == 1 ? "" : "s")
                                + " for the bus saves ~₹" + String.format("%.0f", weeklySavings) + "/week.",
                        "⚡ Potential Savings: ₹" + String.format("%.0f", weeklySavings) + "/week",
                        null, null));
            }
        }

        double subsSpent = byCategoryThisWeek(Category.SUBSCRIPTIONS);
        if (subsSpent > 0) {
            cards.add(new TipCard(TipCard.Urgency.PURPLE, "🔔", "Subscription Check",
                    "You spent ₹" + String.format("%.0f", subsSpent) + " on subscriptions this week. Track monthly auto-debits?",
                    null, "View report", "open_report"));
        } else {
            double entTotal = byCategoryThisWeek(Category.ENTERTAINMENT);
            if (entTotal > 0) {
                cards.add(new TipCard(TipCard.Urgency.PURPLE, "🎬", "Entertainment Check",
                        "You spent ₹" + String.format("%.0f", entTotal) + " on entertainment this week. Track monthly auto-debits?",
                        null, null, null));
            }
        }

        return cards;
    }

    private double byCategoryThisWeek(Category category) {
        List<Transaction> txns = transactions.getValue();
        long ws = weekStart.getValue() != null ? weekStart.getValue() : currentMonday();
        double total = 0;
        if (txns != null) {
            for (Transaction t : txns) {
                if (t.timestamp >= ws && t.category == category) total += t.amount;
            }
        }
        return total;
    }

    private void persist() {
        SharedPreferences.Editor ed = prefs.edit();

        StringBuilder sb = new StringBuilder();
        List<Transaction> txns = transactions.getValue();
        if (txns != null) {
            for (Transaction t : txns) {
                if (sb.length() > 0) sb.append(";");
                sb.append(t.id).append("|").append(t.amount).append("|")
                  .append(t.category.name()).append("|")
                  .append(t.description.replace("|", "")).append("|")
                  .append(t.timestamp);
            }
        }
        ed.putString(KEY_TXNS, sb.toString());

        StringBuilder bsb = new StringBuilder();
        Map<Category, Double> b = budgets.getValue();
        if (b != null) {
            for (Map.Entry<Category, Double> e : b.entrySet()) {
                if (bsb.length() > 0) bsb.append(";");
                bsb.append(e.getKey().name()).append("=").append(e.getValue());
            }
        }
        ed.putString(KEY_BUDGETS, bsb.toString());

        ed.putFloat(KEY_JAR,    roundUpJar.getValue() != null ? roundUpJar.getValue().floatValue() : 0f);
        ed.putInt(KEY_STREAK,   streakWeeks.getValue() != null ? streakWeeks.getValue() : 0);
        ed.putLong(KEY_WEEK,    weekStart.getValue() != null ? weekStart.getValue() : currentMonday());

        StringBuilder gsb = new StringBuilder();
        List<SavingsGoal> goals = savingsGoals.getValue();
        if (goals != null) {
            for (SavingsGoal g : goals) {
                if (gsb.length() > 0) gsb.append(";");
                gsb.append(g.id).append("|").append(g.name.replace("|", "")).append("|")
                   .append(g.target).append("|").append(g.saved);
            }
        }
        ed.putString(KEY_GOALS, gsb.toString());

        ed.apply();
    }

    private void load() {
        String txnRaw = prefs.getString(KEY_TXNS, null);
        if (txnRaw != null && !txnRaw.isEmpty()) {
            List<Transaction> list = new ArrayList<>();
            for (String line : txnRaw.split(";")) {
                String[] p = line.split("\\|");
                if (p.length < 5) continue;
                try {
                    list.add(new Transaction(
                            Long.parseLong(p[0]),
                            Double.parseDouble(p[1]),
                            Category.valueOf(p[2]),
                            p[3],
                            Long.parseLong(p[4])
                    ));
                } catch (Exception ignored) {}
            }
            transactions.setValue(list);
        }

        String budgetRaw = prefs.getString(KEY_BUDGETS, null);
        if (budgetRaw != null && !budgetRaw.isEmpty()) {
            Map<Category, Double> b = new HashMap<>(DEFAULT_BUDGETS);
            for (String entry : budgetRaw.split(";")) {
                String[] p = entry.split("=");
                if (p.length != 2) continue;
                try { b.put(Category.valueOf(p[0]), Double.parseDouble(p[1])); }
                catch (Exception ignored) {}
            }
            budgets.setValue(b);
        }

        roundUpJar.setValue((double) prefs.getFloat(KEY_JAR, 0f));
        streakWeeks.setValue(prefs.getInt(KEY_STREAK, 0));
        weekStart.setValue(prefs.getLong(KEY_WEEK, currentMonday()));

        String goalsRaw = prefs.getString(KEY_GOALS, null);
        if (goalsRaw != null && !goalsRaw.isEmpty()) {
            List<SavingsGoal> list = new ArrayList<>();
            for (String line : goalsRaw.split(";")) {
                String[] p = line.split("\\|");
                if (p.length < 4) continue;
                try {
                    list.add(new SavingsGoal(
                            Long.parseLong(p[0]), p[1],
                            Double.parseDouble(p[2]), Double.parseDouble(p[3])
                    ));
                } catch (Exception ignored) {}
            }
            
            boolean healed = false;
            for (SavingsGoal g : list) {
                if (g.target <= 0) { g.target = DEFAULT_GENERAL_GOAL_TARGET; healed = true; }
            }
            savingsGoals.setValue(list);
            if (healed) persist();
        }
    }

    private static long currentMonday() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() > System.currentTimeMillis())
            cal.add(Calendar.DAY_OF_MONTH, -7);
        return cal.getTimeInMillis();
    }
}
