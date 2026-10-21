package com.example.data

import com.example.model.*

object VeloraData {

    // Current Logged-in User Profile
    val currentUser = UserProfile(
        id = "user_me",
        name = "Alif Islam",
        age = 24,
        gender = "Man",
        city = "Dhaka",
        country = "Bangladesh",
        distanceKm = 0,
        profession = "Software Engineer",
        height = "5'10\" (178 cm)",
        relationshipStatus = "Single",
        aboutMe = "Curious mind, coffee enthusiast, and avid traveler. Looking for genuine conversations and romantic connections.",
        interests = listOf("Travel", "Photography", "Music", "Coffee", "Tech"),
        photos = listOf(
            "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=800&q=80"
        ),
        isVerified = true,
        isOnline = true,
        email = "alif.islam@example.com",
        phone = "+880 1712-345678"
    )

    // Curated Discover Profiles (Including exact profiles from the user's reference mockup)
    val discoverProfiles = mutableListOf(
        UserProfile(
            id = "user_ayesha",
            name = "Ayesha",
            age = 22,
            gender = "Woman",
            city = "Dhaka",
            country = "Bangladesh",
            distanceKm = 2,
            profession = "Student",
            height = "5'4\" (163 cm)",
            relationshipStatus = "Single",
            aboutMe = "I love traveling, good food, music and meeting new people. Let's create some beautiful memories together. \uD83D\uDE42",
            interests = listOf("Travel", "Music", "Photography", "Cafes", "Art"),
            photos = listOf(
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=800&q=80"
            ),
            isVerified = true,
            isOnline = true,
            lastActive = "Online now",
            joinedDate = "12 Oct 2026",
            email = "ayesha123@gmail.com"
        ),
        UserProfile(
            id = "user_nusrat",
            name = "Nusrat",
            age = 21,
            gender = "Woman",
            city = "Sylhet",
            country = "Bangladesh",
            distanceKm = 14,
            profession = "Fashion Designer",
            height = "5'3\" (160 cm)",
            relationshipStatus = "Looking for something special",
            aboutMe = "Creative soul with a passion for traditional fabrics and contemporary design. Coffee and book lover.",
            interests = listOf("Fashion", "Design", "Literature", "Tea", "Movies"),
            photos = listOf(
                "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?auto=format&fit=crop&w=800&q=80"
            ),
            isVerified = true,
            isOnline = true,
            lastActive = "2h ago",
            joinedDate = "09 Oct 2026",
            email = "nusrat@gmail.com"
        ),
        UserProfile(
            id = "user_tania",
            name = "Tania",
            age = 24,
            gender = "Woman",
            city = "Rajshahi",
            country = "Bangladesh",
            distanceKm = 28,
            profession = "Architect",
            height = "5'5\" (165 cm)",
            relationshipStatus = "Single",
            aboutMe = "Passionate about sustainable architecture, plants, and weekend drives. Let's talk about dreams and sunsets.",
            interests = listOf("Architecture", "Nature", "Painting", "Cycling"),
            photos = listOf(
                "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1508214751196-bcfd4ca60f91?auto=format&fit=crop&w=800&q=80"
            ),
            isVerified = false,
            isOnline = false,
            lastActive = "4h ago",
            joinedDate = "07 Oct 2026",
            email = "tania@gmail.com"
        ),
        UserProfile(
            id = "user_sadia",
            name = "Sadia",
            age = 27,
            gender = "Woman",
            city = "Khulna",
            country = "Bangladesh",
            distanceKm = 45,
            profession = "Doctor",
            height = "5'6\" (168 cm)",
            relationshipStatus = "Single",
            aboutMe = "Healthcare worker, morning runner, and amateur violinist. Looking for genuine connection with good humor.",
            interests = listOf("Fitness", "Music", "Cooking", "Medicine", "Pets"),
            photos = listOf(
                "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?auto=format&fit=crop&w=800&q=80"
            ),
            isVerified = true,
            isOnline = false,
            lastActive = "1d ago",
            joinedDate = "06 Oct 2026",
            email = "sadia@gmail.com"
        ),
        UserProfile(
            id = "user_rafi",
            name = "Rafi",
            age = 26,
            gender = "Man",
            city = "Chittagong",
            country = "Bangladesh",
            distanceKm = 18,
            profession = "Financial Analyst",
            height = "5'11\" (180 cm)",
            relationshipStatus = "Single",
            aboutMe = "Outdoor lover, beach walks, and cricket fan. Looking for a partner to explore life with.",
            interests = listOf("Sports", "Economics", "Hiking", "Cinema"),
            photos = listOf(
                "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=800&q=80"
            ),
            isVerified = true,
            isOnline = true,
            lastActive = "Online now",
            joinedDate = "10 Oct 2026",
            email = "rafi@gmail.com"
        ),
        UserProfile(
            id = "user_ibrahim",
            name = "Ibrahim",
            age = 25,
            gender = "Man",
            city = "Dhaka",
            country = "Bangladesh",
            distanceKm = 8,
            profession = "Cinematographer",
            height = "5'9\" (175 cm)",
            relationshipStatus = "Single",
            aboutMe = "Telling stories through visuals. Coffee and late night conversations are my jam.",
            interests = listOf("Films", "Photography", "Indie Music", "Coffee"),
            photos = listOf(
                "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?auto=format&fit=crop&w=800&q=80"
            ),
            isVerified = false,
            isOnline = false,
            lastActive = "2d ago",
            joinedDate = "04 Oct 2026",
            email = "ibrahim@gmail.com"
        )
    )

    // Initial Matches matching mockup screen 8
    val initialMatches = listOf(
        MatchItem(
            id = "match_ayesha",
            user = discoverProfiles[0],
            matchedAt = "Today",
            lastMessage = "Hi! Nice to meet you \uD83D\uDE0A",
            lastMessageTime = "10:31 AM",
            unreadCount = 2,
            isOnline = true,
            isNew = true
        ),
        MatchItem(
            id = "match_nusrat",
            user = discoverProfiles[1],
            matchedAt = "Today",
            lastMessage = "You matched! You both like literature.",
            lastMessageTime = "2h ago",
            unreadCount = 0,
            isOnline = true,
            isNew = true
        ),
        MatchItem(
            id = "match_tania",
            user = discoverProfiles[2],
            matchedAt = "Yesterday",
            lastMessage = "Nice to meet you! How's your week?",
            lastMessageTime = "4h ago",
            unreadCount = 0,
            isOnline = false,
            isNew = false
        ),
        MatchItem(
            id = "match_rafi",
            user = discoverProfiles[4],
            matchedAt = "2 days ago",
            lastMessage = "You matched! Say hello to Rafi",
            lastMessageTime = "1d ago",
            unreadCount = 0,
            isOnline = true,
            isNew = false
        ),
        MatchItem(
            id = "match_sadia",
            user = discoverProfiles[3],
            matchedAt = "3 days ago",
            lastMessage = "See you soon! \uD83E\uDEF6",
            lastMessageTime = "1d ago",
            unreadCount = 0,
            isOnline = false,
            isNew = false
        ),
        MatchItem(
            id = "match_ibrahim",
            user = discoverProfiles[5],
            matchedAt = "5 days ago",
            lastMessage = "Matched with you",
            lastMessageTime = "2d ago",
            unreadCount = 0,
            isOnline = false,
            isNew = false
        )
    )

    // Chat messages matching mockup screen 9 (Ayesha & Alif)
    val ayeshaChatHistory = listOf(
        ChatMessage(
            id = "m1",
            senderId = "user_ayesha",
            text = "Hey! \uD83D\uDE0A",
            timestamp = "10:24 AM",
            isFromMe = false
        ),
        ChatMessage(
            id = "m2",
            senderId = "user_me",
            text = "Hi! Nice to meet you \uD83D\uDE0A",
            timestamp = "10:31 AM",
            isFromMe = true
        ),
        ChatMessage(
            id = "m3",
            senderId = "user_ayesha",
            text = "Same here! Where are you from?",
            timestamp = "10:36 AM",
            isFromMe = false
        ),
        ChatMessage(
            id = "m4",
            senderId = "user_me",
            text = "I'm from Dhaka. And you?",
            timestamp = "10:37 AM",
            isFromMe = true
        ),
        ChatMessage(
            id = "m5",
            senderId = "user_ayesha",
            text = "I'm from Chittagong. \uD83D\uDE09",
            timestamp = "10:40 AM",
            isFromMe = false
        )
    )

    // Notifications matching mockup screen 14
    val initialNotifications = listOf(
        NotificationItem(
            id = "notif_1",
            title = "New Match!",
            message = "You matched with Ayesha!",
            timeAgo = "2m ago",
            type = "MATCH",
            avatarUrl = discoverProfiles[0].photos.firstOrNull(),
            isRead = false
        ),
        NotificationItem(
            id = "notif_2",
            title = "Message",
            message = "Nusrat sent you a message.",
            timeAgo = "10m ago",
            type = "MESSAGE",
            avatarUrl = discoverProfiles[1].photos.firstOrNull(),
            isRead = false
        ),
        NotificationItem(
            id = "notif_3",
            title = "Profile Viewed",
            message = "Someone viewed your profile.",
            timeAgo = "29m ago",
            type = "VIEW",
            isRead = true
        ),
        NotificationItem(
            id = "notif_4",
            title = "Like",
            message = "Tania liked your profile.",
            timeAgo = "2h ago",
            type = "LIKE",
            avatarUrl = discoverProfiles[2].photos.firstOrNull(),
            isRead = true
        ),
        NotificationItem(
            id = "notif_5",
            title = "New Visitor",
            message = "A new visitor checked your profile.",
            timeAgo = "3h ago",
            type = "VIEW",
            isRead = true
        )
    )

    // Reports matching mockup admin panel Reports & Moderation
    val initialReports = listOf(
        UserReport(
            id = "rep_1",
            type = "Profile",
            reporterName = "Ayesha",
            reportedUserId = "user_rafi123",
            reportedUserName = "Rafi123",
            reason = "Fake profile",
            date = "12 Oct 2026",
            status = "Pending"
        ),
        UserReport(
            id = "rep_2",
            type = "Message",
            reporterName = "Nusrat",
            reportedUserId = "user_imran456",
            reportedUserName = "Imran456",
            reason = "Harassment",
            date = "11 Oct 2026",
            status = "Under Review"
        ),
        UserReport(
            id = "rep_3",
            type = "Content",
            reporterName = "Tania",
            reportedUserId = "user_sadia789",
            reportedUserName = "Sadia789",
            reason = "Inappropriate photo",
            date = "11 Oct 2026",
            status = "Pending"
        ),
        UserReport(
            id = "rep_4",
            type = "Profile",
            reporterName = "Sadia",
            reportedUserId = "user_riyad",
            reportedUserName = "Riyad",
            reason = "Scam",
            date = "10 Oct 2026",
            status = "Resolved"
        ),
        UserReport(
            id = "rep_5",
            type = "Message",
            reporterName = "Rafi",
            reportedUserId = "user_mohana",
            reportedUserName = "Mohana",
            reason = "Spam",
            date = "10 Oct 2026",
            status = "Dismissed"
        ),
        UserReport(
            id = "rep_6",
            type = "Profile",
            reporterName = "Trisha",
            reportedUserId = "user_arif123",
            reportedUserName = "Arif123",
            reason = "Fake profile",
            date = "09 Oct 2026",
            status = "Resolved"
        )
    )

    // Premium Subscription Plans matching mockup screen 12
    val subscriptionPlans = listOf(
        SubscriptionPlan(
            id = "plan_1m",
            name = "1 Month",
            durationMonths = 1,
            priceBdt = 299,
            features = listOf(
                "See who liked you",
                "Unlimited likes",
                "Advanced filters",
                "Ad-free experience",
                "1 Free Boost per month"
            )
        ),
        SubscriptionPlan(
            id = "plan_3m",
            name = "3 Months",
            durationMonths = 3,
            priceBdt = 699,
            originalPriceBdt = 897,
            savingsPercent = 22,
            features = listOf(
                "See who liked you",
                "Unlimited likes",
                "Advanced filters",
                "Ad-free experience",
                "2 Free Boosts per month",
                "5 Super Likes per week"
            )
        ),
        SubscriptionPlan(
            id = "plan_12m",
            name = "12 Months",
            durationMonths = 12,
            priceBdt = 1199,
            originalPriceBdt = 3588,
            savingsPercent = 66,
            isPopular = true,
            features = listOf(
                "See who liked you",
                "Unlimited likes",
                "Advanced filters",
                "Ad-free experience",
                "Unlimited Boosts",
                "Unlimited Super Likes",
                "VIP Profile Badge",
                "Priority Support"
            )
        )
    )

    // Language Options matching mockup screen 4
    val languages = listOf(
        LanguageOption("bn", "Bangla", "বাংলা", "\uD83C\uDDE7\uD83C\uDDE9"),
        LanguageOption("en", "English", "English", "\uD83C\uDF10"),
        LanguageOption("hi", "Hindi", "हिन्दी", "\uD83C\uDE36"),
        LanguageOption("ar", "Arabic", "العربية", "\uD83C\uDDF8\uD83C\uDDE6"),
        LanguageOption("es", "Spanish", "Español", "\uD83C\uDDEA\uD83C\uDDF8"),
        LanguageOption("fr", "French", "Français", "\uD83C\uDDE8\uD83C\uDDF7")
    )
}
