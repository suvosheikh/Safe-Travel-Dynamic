# Implementation Plan: Location Accuracy, Sorting & Loading UX

The goal is to improve location name granularity, fix nearest-first sorting by waiting for a valid GPS lock, and implement a smooth "Locating -> Shimmer -> Content" transition.

## User Review Required

> [!IMPORTANT]
> - **Wait for GPS:** স্ক্রিনে ঢোকার পর জিপিএস লক না পাওয়া পর্যন্ত এটি "Locating..." মোডে থাকবে এবং স্কেলিটন শিমার (Skeleton Shimmer) দেখাবে। এতে করে ডাটা যখন আসবে, তখন সেটি একদম সঠিক দূরত্ব অনুযায়ী সাজানো থাকবে।
> - **Granular Names:** আমি লোকেশন ডিটেকশন সিস্টেম আপডেট করব যাতে এটি "তেজগাঁও"-এর মতো বড় নামের বদলে আপনার এলাকার নির্দিষ্ট নাম (যেমন: আগারগাঁও) খুঁজে বের করতে পারে।

## Proposed Changes

### 1. Improve Location Granularity
Update the utility to prioritize specific neighborhood names.

#### [MODIFY] [LocationHelper.kt](file:///F:/Android studio/safe-travel/app/src/main/java/com/safetravel/tracker/util/LocationHelper.kt)
- Update `getPlaceName` and `fetchFromNominatim` to prioritize `neighbourhood`, `suburb`, and `road` in a more granular hierarchy.

### 2. Locating & Shimmer Logic
Update the screens to handle the initial "waiting for location" state.

#### [MODIFY] [ThanaScreen.kt](file:///F:/Android studio/safe-travel/app/src/main/java/com/safetravel/tracker/ui/screens/ThanaScreen.kt) & [FireStationScreen.kt](file:///F:/Android studio/safe-travel/app/src/main/java/com/safetravel/tracker/ui/screens/FireStationScreen.kt)
- **Smart Initial Capture:** Wait for the first non-null `userPosState` from the ViewModel.
- **Loading State:** Add an `isLocating` state. While `initialUserPos` is null, `isLocating` will be true.
- **UI Transition:** If `isLocating` or `isLoading` (fetching from DB) is true, show the `ThanaSkeletonCard` list.

### 3. Sorting & Consistency
Ensure the list remains stable and correctly ordered once loaded.

#### [MODIFY] [ThanaScreen.kt](file:///F:/Android studio/safe-travel/app/src/main/java/com/safetravel/tracker/ui/screens/ThanaScreen.kt)
- Ensure the Haversine distance is calculated only once the `initialUserPos` is acquired.
- The `sortedBy` logic will remain nearest-first.

## Verification Plan

### Manual Verification
- **Cold Entry Test:** জিপিএস অফ করে বা ইনডোরে থেকে স্ক্রিনে ঢুকুন। দেখুন এটি শিমার লোডিং দেখাচ্ছে কিনা। এরপর জিপিএস অন করলে ডাটাগুলো সুন্দরভাবে পপুলেট হচ্ছে কিনা চেক করুন।
- **Sorting Test:** নিশ্চিত করুন যে প্রথম আইটেমটিই আপনার সবচেয়ে কাছে।
- **Name Accuracy:** আপনার নির্দিষ্ট এলাকার নাম (Neighbourhood) সঠিক আসছে কিনা যাচাই করুন।
