# Call History App - Paging3 Implementation Guide

## Problem Summary
1. PagingSource was created but not integrated with ViewModel
2. Broadcast receiver triggered refresh but UI didn't update
3. Manual pagination was implemented instead of using Paging3
4. No reactive connection between ContentResolver updates and UI

## Solution Overview

### Architecture Flow
```
CallBroadcastReceiver → Repository.triggerRefresh() → ViewModel observes refresh trigger → 
PagingSource fetches from ContentResolver → UI updates automatically
```

## Key Changes Made

### 1. CallHistoryRepository.kt
**Changes:**
- Replaced `MutableSharedFlow` with `MutableStateFlow<Long>` for refresh trigger
- Added `getCallHistoryPager()` method that returns `Flow<PagingData<CallEntity>>`
- Removed manual data fetching methods (getAllCalls, getAllCallsNew)
- Pager configuration: pageSize=50, no placeholders

**Why:**
- StateFlow with timestamp ensures every refresh creates a new PagingSource
- Paging3 handles all pagination logic automatically
- ContentResolver queries happen in PagingSource, not Repository

### 2. CallHistoryViewModel.kt
**Changes:**
- Removed manual pagination logic (currentPage, pageSize, loadMoreItems)
- Removed _allCallHistory and _displayedCalls StateFlows
- Added `callHistoryPagingData` Flow that combines:
  - UI state (filter selection)
  - Repository refresh trigger
- Uses `flatMapLatest` to recreate pager when filter or refresh changes
- Filters applied via `pagingData.filter()` for search and day filters
- Added `cachedIn(viewModelScope)` for proper lifecycle management

**Why:**
- Paging3 handles all loading states automatically
- Reactive to both user actions (filter changes) and system events (broadcast)
- Memory efficient - only loads visible items

### 3. CallHistory.kt (Screen)
**Changes:**
- Replaced `List<CallEntity>` with `LazyPagingItems<CallEntity>`
- Removed manual load more logic
- Changed from `itemsIndexed()` to `items(count, key)` with paging
- Used `collectAsLazyPagingItems()` to collect paging data

**Why:**
- LazyPagingItems automatically handles loading more items
- Built-in loading states (refresh, append, prepend)
- Efficient memory usage with virtual scrolling

### 4. CallBroadcastReceiver.kt
**No changes needed** - Already correctly triggers `repository.triggerRefresh()`

## How It Works

### Normal Flow (User Opens App)
1. ViewModel creates `callHistoryPagingData` Flow
2. Flow observes `selectedFilter` and `refreshTrigger`
3. PagingSource queries ContentResolver with filter
4. First 50 items loaded and displayed
5. User scrolls → Paging3 automatically loads next page
6. Filters applied in-memory via `filter()` operator

### Broadcast Flow (New Call Received)
1. Phone call ends → `CallBroadcastReceiver.onReceive()` triggered
2. Receiver calls `repository.triggerRefresh()`
3. Repository updates `_refreshTrigger` StateFlow with new timestamp
4. ViewModel's `combine()` operator detects change
5. `flatMapLatest` cancels old pager and creates new one
6. New PagingSource queries ContentResolver (gets latest data)
7. UI automatically updates with new call at top

### Filter Change Flow
1. User clicks filter chip (e.g., "Missed")
2. `updateFilter("missed")` called
3. `_uiState` updated with new filter
4. `combine()` emits new filter value
5. `flatMapLatest` creates new pager with filter
6. PagingSource queries with WHERE clause for missed calls
7. Additional filters (search, day) applied via `filter()` operator

## Benefits

### Performance
- Only loads visible items (50 at a time)
- Efficient ContentResolver queries with LIMIT/OFFSET
- No loading entire call history into memory

### Reactivity
- Automatic UI updates when new calls arrive
- Smooth filter transitions
- No manual refresh needed

### Code Quality
- Less boilerplate (no manual pagination)
- Separation of concerns (Repository → PagingSource → ViewModel → UI)
- Testable components

## Testing Checklist

- [ ] App opens and shows call history
- [ ] Scroll down loads more items automatically
- [ ] Filter by "Missed" shows only missed calls
- [ ] Filter by "Received" shows only received calls
- [ ] Filter by "Dialed" shows only dialed calls
- [ ] Search by number filters correctly
- [ ] Search by contact name filters correctly
- [ ] Day filter works (e.g., show only Monday calls)
- [ ] Receive a call → End call → New call appears at top
- [ ] Make a call → End call → New call appears at top
- [ ] Miss a call → New call appears at top
- [ ] Combine filters (e.g., Missed + Monday + Search)

## Troubleshooting

### Issue: UI doesn't update after call
**Check:**
1. Is CallBroadcastReceiver registered in AndroidManifest.xml?
2. Does receiver have PHONE_STATE permission?
3. Is `AppRepositoryProvider.repository` initialized before broadcast?

### Issue: Filters not working
**Check:**
1. Is `matchesUiFilters()` logic correct?
2. Are contact names cached before filtering?
3. Is search query trimmed properly?

### Issue: Duplicate items or wrong order
**Check:**
1. Is `CallLog.Calls.DATE DESC` in PagingSource query?
2. Is item key unique? (using `call.id`)
3. Is `distinctUntilChanged()` preventing unnecessary recreations?

## Future Enhancements

1. **Loading States**: Show loading indicator using `pagingData.loadState`
2. **Error Handling**: Handle ContentResolver errors gracefully
3. **Empty State**: Show "No calls" when list is empty
4. **Pull to Refresh**: Add SwipeRefresh to manually trigger refresh
5. **RemoteMediator**: If syncing with server in future
6. **Prefetch Distance**: Tune `prefetchDistance` in PagingConfig
7. **Jump to Top**: Add FAB to scroll to top after new call

## Code References

- **PagingSource**: `CallHistoryPagingSource.kt` - Queries ContentResolver
- **Repository**: `CallHistoryRepository.kt` - Creates Pager
- **ViewModel**: `CallHistoryViewModel.kt` - Manages UI state and paging
- **Screen**: `CallHistory.kt` - Displays LazyPagingItems
- **Receiver**: `CallBroadcastReceiver.kt` - Triggers refresh
