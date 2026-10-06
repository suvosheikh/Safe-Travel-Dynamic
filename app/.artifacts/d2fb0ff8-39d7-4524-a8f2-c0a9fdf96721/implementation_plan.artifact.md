# Implementation Plan - Emergency Contact UI/UX Premium Upgrade

এই প্ল্যানের মাধ্যমে "Emergency Contacts" স্ক্রিনটিকে আরও প্রিমিয়াম, আধুনিক এবং কার্যকর করা হবে। এখানে নিওন ডিজাইন, স্মুথ অ্যানিমেশন এবং নতুন অফিসিয়াল বটম শিট যুক্ত করা হবে।

## User Review Required

> [!IMPORTANT]
> **Neon Glow**: আমরা কার্ডগুলোতে একটি হালকা গ্লোয়িং বর্ডার যোগ করবো যা ডার্ক মোডে অ্যাপটিকে আরও লাক্সারি লুক দিবে।
> **Animated Tabs**: ট্যাব পরিবর্তনের সময় কন্টেন্টগুলো হঠাৎ না এসে স্লাইড হয়ে আসবে।
> **Official Bottom Sheet**: অফিসিয়াল নম্বরে ক্লিক করলে সরাসরি কল না গিয়ে একটি ডিটেইলড শিট ওপেন হবে যাতে ইউজার আরও তথ্য পায়।

## Official Bottom Sheet UI/UX Plan
অফিসিয়াল হেল্পলাইন বটম শিটে যা যা থাকবে:
- **Service Identity**: বড় একটি নিওন আইকন (যেমন: ৯৯৯-এর জন্য শিল্ড)।
- **Quick Dial Button**: একটি বিশাল **"DIAL [NUMBER]"** বাটন যা পুরো স্ক্রিন জুড়ে থাকবে।
- **Preparation Tips**: কল করার আগে কী কী তথ্য তৈরি রাখা উচিত তার ২-৩টি ছোট পয়েন্ট (যেমন: "শান্ত থাকুন", "আপনার লোকেশনটি নির্ভুলভাবে বলুন")।
- **Language Toggle**: টিপসগুলো বাংলা এবং ইংরেজিতে দেখার সুবিধা।

## Proposed Changes

### 1. ViewModel Logic
#### [MODIFY] [SafeTravelViewModel.kt](file:///F:/Android%20studio/safe-travel/app/src/main/java/com/safetravel/tracker/viewmodel/SafeTravelViewModel.kt)
- `emergencySearchQuery` নামে একটি নতুন StateFlow যোগ করা হবে।
- সার্চ কুয়েরি অনুযায়ী `hotlines` এবং `userGuardians` ফিল্টার করার লজিক।

### 2. UI Components & Screen
#### [MODIFY] [EmergencyContactScreen.kt](file:///F:/Android%20studio/safe-travel/app/src/main/java/com/safetravel/tracker/ui/screens/EmergencyContactScreen.kt)
- **Search Bar**: স্ক্রিনের উপরে একটি Glassmorphic সার্চ বার যোগ করা।
- **AnimatedContent**: ট্যাব পরিবর্তনের জন্য ফেড/স্লাইড অ্যানিমেশন যুক্ত করা।
- **Neon Cards**: কার্ডগুলোতে `BorderStroke` এবং নিওন কালার (Red/Blue) গ্লো যুক্ত করা।
- **Hero Card**: ৯৯৯ নম্বরটিকে অফিসিয়াল লিস্টের সবার উপরে হাইলাইটেড ভাবে দেখানো।
- **Guardian SMS**: পার্সোনাল কন্টাক্টে কল বাটনের পাশে একটি SMS বাটন যোগ করা যা ইউজারের লোকেশন সহ মেসেজ সেন্ড করবে।
- **OfficialHotlineBottomSheet**: একটি নতুন কম্পোজেবল যা অফিসিয়াল হেল্পলাইনের ডিটেইলস এবং টিপস দেখাবে।

## Verification Plan

### Manual Verification
1. Emergency Contacts স্ক্রিনে গিয়ে উপরের সার্চ বারটি পরীক্ষা করা।
2. ট্যাব পরিবর্তন করে স্মুথ অ্যানিমেশন লক্ষ্য করা।
3. অফিসিয়াল হেল্পলাইন (যেমন: ৯৯৯) এ ক্লিক করে নতুন বটম শিটটি চেক করা।
4. পার্সোনাল কন্টাক্টের SMS বাটনে ক্লিক করে মেসেজ অ্যাপ ওপেন হচ্ছে কি না দেখা।
5. কল বাটনগুলোতে ক্লিক করে হ্যাপটিক ফিডব্যাক (ভাইব্রেশন) অনুভব করা।
