package com.martin.storage.data.receipt

import android.util.Log

object ReceiptParser {

    private const val TAG = "ReceiptParser"

    private val groceryItems = setOf(

        // =====================
        // Fruits
        // =====================
        "apple","banana","orange","mandarin","tangerine","lemon","lime",
        "grape","grapes","strawberry","blueberry","raspberry","blackberry",
        "pear","peach","nectarine","plum","mango","kiwi","kiwifruit",
        "pineapple","watermelon","rockmelon","melon","avocado",

        // =====================
        // Vegetables
        // =====================
        "potato","kumara","sweet potato","onion","red onion","brown onion",
        "carrot","broccoli","cauliflower","cabbage","lettuce","spinach",
        "silverbeet","celery","capsicum","pepper","chilli","cucumber",
        "tomato","cherry tomato","zucchini","courgette","pumpkin",
        "garlic","ginger","spring onion", "mushroom", "white", "brown", "red",

        // =====================
        // Meat / Protein
        // =====================
        "chicken","chicken breast","chicken thigh","beef","mince",
        "pork","lamb","steak","sausage","sausages","bacon",
        "ham","salmon","fish","tuna","shrimp","prawns","seafood",
        "tofu","tempeh","eggs","egg",

        // =====================
        // Dairy
        // =====================
        "milk","almond milk","soy milk","cheese","butter","yogurt",
        "yoghurt","cream","sour cream","custard", "yog", "greek",

        // =====================
        // Bread / Bakery
        // =====================
        "bread","wholemeal bread","white bread","rolls","baguette",
        "muffin","croissant","toast","buns",

        // =====================
        // Pantry Items
        // =====================
        "rice","basmati rice","jasmine rice","pasta","spaghetti",
        "noodles","oats","cereal","flour","sugar","salt","pepper",
        "honey","jam","peanut butter","chocolate","biscuits",
        "cookies","crackers","chips","popcorn",

        // =====================
        // Drinks
        // =====================
        "water","sparkling water","juice","orange juice","apple juice",
        "coffee","tea","cola","soda","soft drink","energy drink",

        // =====================
        // Household Items
        // =====================
        "detergent","laundry detergent","washing powder","washing liquid",
        "liquid","dishwash","soap","hand soap","body wash",
        "shampoo","conditioner","toilet paper","tissue","tissues",
        "paper towel","paper towels","cleaner","surface cleaner",
        "bleach","sponge","sponges","bin bags","garbage bags",
        "trash bags","cling wrap","foil","aluminium foil", "toilet", "wipe",

        // =====================
        // Frozen Foods
        // =====================
        "frozen","frozen vegetables","frozen peas","ice cream",
        "frozen pizza","frozen chips",

        // =====================
        // Cooking Oils
        // =====================
        "olive oil","extra virgin olive oil","vegetable oil","canola oil",
        "sunflower oil","coconut oil","sesame oil","avocado oil",
        "peanut oil","rice bran oil", "oil",

        // =====================
        // Sauces
        // =====================
        "tomato sauce","ketchup","mustard","soy sauce","dark soy sauce",
        "light soy sauce","oyster sauce","fish sauce","bbq sauce",
        "barbecue sauce","chilli sauce","hot sauce","sriracha",
        "mayonnaise","mayo","aioli","hollandaise",
        "pasta sauce","pizza sauce","teriyaki sauce",
        "sweet chilli sauce","worcestershire sauce", "soy",

        // =====================
        // Cooking Basics
        // =====================
        "salt","sea salt","table salt","rock salt",
        "pepper","black pepper","white pepper",
        "sugar","brown sugar","raw sugar",
        "honey","maple syrup",

        // =====================
        // Herbs & Spices
        // =====================
        "garlic powder","onion powder","curry powder",
        "turmeric","paprika","chilli flakes","cumin",
        "coriander","oregano","basil","thyme","rosemary",
        "ginger","cinnamon","nutmeg",

        // =====================
        // Cooking Essentials
        // =====================
        "flour","plain flour","self raising flour",
        "corn flour","cornstarch",
        "baking powder","baking soda","yeast",
        "stock","chicken stock","beef stock","vegetable stock",

        // =====================
        // Condiments & Extras
        // =====================
        "vinegar","balsamic vinegar","white vinegar","apple cider vinegar",
        "pickles","gherkins","olives","capers",
        "jam","peanut butter","spread"
    )

    private val ignoreWords = setOf(

        // totals
        "total","subtotal","sub total","grand total","balance","amount due",
        "amount","change","cash","tender","payment","paid","due",

        // tax
        "gst","gst incl","gst included","tax","tax total","tax invoice",

        // payment types
        "visa","mastercard","eftpos","paywave","pay pass","debit",
        "credit","card","contactless",

        // transaction info
        "transaction","trans","approval","auth","authorisation","aid",
        "terminal","terminal id","merchant","merchant id",
        "trace","reference","ref","invoice","receipt",

        // store metadata
        "store","store id","branch","location","operator","operator id",
        "cashier","served by","staff","pos","lane","till","register",

        // date & time
        "date","time","timestamp",

        // loyalty / promotions
        "clubcard","everyday rewards","rewards","points","bonus points",
        "promotion","promo","special","saving","savings","discount",

        // receipt formatting
        "qty","item","description","price","unit price","amount",
        "items","no.","number",

        // refund / adjustments
        "refund","void","cancel","adjustment","correction",

        // footer messages
        "thank you","thanks","see you again","keep this receipt",
        "customer copy","merchant copy","please retain",

        // misc system lines
        "barcode","scan","code","batch","sequence","seq","entry",
        "printed","copy","duplicate",

        // online / digital
        "online","order","pickup","delivery","click and collect",

        // rounding
        "rounding","round off"
    )

    fun parseLines(lines: List<String>): List<ParsedItem> {

        val items = mutableListOf<ParsedItem>()

        for (line in lines) {
            Log.d(TAG, line)

            val lower = line.lowercase()

            if (!groceryItems.any { lower.contains(it) }) continue

            val words = line.split(" ")

            val name = words
                .filterNot {ignoreWords.contains(it)}
                .joinToString(" ")
                .trim()

            if (name.isBlank()) continue

            items.add(
                ParsedItem(
                    name = name,
                    quantity = 0.0,
                    unit = ""
                )
            )
        }

        return items
    }
}