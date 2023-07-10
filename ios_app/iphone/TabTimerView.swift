import SwiftUI
import shared

private let emojiHPadding = 10.0
private let emojiWidth = 30.0
private let emojiStartPadding = emojiWidth + (emojiHPadding * 2)

private let activityItemShape = RoundedRectangle(cornerRadius: 16, style: .continuous)

struct TabTimerView: View {

    @State private var vm = TabTimerVM()

    @State private var isReadmePresented = false

    @State private var isAddActivityPresented = false
    @State private var isEditActivitiesPresented = false
    @State private var isSettingsSheetPresented = false
    @State private var isSummaryPresented = false
    @State private var isHistoryPresented = false

    var body: some View {

        // Without NavigationView the NavigationLink does not work,
        // remember the .navigationBarHidden(true)
        NavigationView {

            // If outside of the NavigationView - deletion swipe does not opened in full size.
            VMView(vm: vm) { state in

                ZStack {

                    Color(.bg).edgesIgnoringSafeArea(.all)

                    VStack {

                        //
                        // Progress

                        ZStack(alignment: .top) {

                            HStack {

                                // todo
                                //                            EditButton()
                                //                                    .padding(.leading, 20)

                                //                            if !isShowReadmeOnMain.toBoolean() {
                                Spacer()
                                //                            }

                                /*
                            if isShowReadmeOnMain.toBoolean() {
                                Spacer()
                                Button("Readme") {
                                    isReadmePresented = true
                                }
                                        .padding(.trailing, 20)
                            }
                             */
                            }
                                    .padding(.top, 6)

                            //////
                        }

                        //
                        // List

                        ScrollView(.vertical, showsIndicators: false) {

                            VStack {

                                ZStack {
                                }
                                        .frame(height: 20)

                                VStack {

                                    let activitiesUI = state.activitiesUI

                                    ForEach(activitiesUI, id: \.activity.id) { activityUI in
                                        TabTimerView_ActivityRowView(
                                                activityUI: activityUI,
                                                lastInterval: state.lastInterval,
                                                withTopDivider: activityUI.withTopDivider
                                        )
                                    }
                                }
                                /*
                                    .onDelete { set in
                                        // to show the button
                                    }
                                    .onMove { set, toIdx in
                                        if (set.count != 1) {
                                            fatalError("bad count for moving \(set.count)")
                                        }

                                        var tempList = activities.map {
                                            $0
                                        }

                                        let fromIdx = set.first!
                                        if fromIdx < toIdx {
                                            tempList.insert(tempList[fromIdx], at: toIdx)
                                            tempList.remove(at: fromIdx)
                                        } else {
                                            let removedTask = tempList.remove(at: fromIdx)
                                            tempList.insert(removedTask, at: toIdx)
                                        }

                                        for (tempIndex, tempTask) in tempList.enumerated() {
                                            tempTask.updateSort(tempIndex)
                                        }
                                    }
                                     */

                                HStack(spacing: 20) {

                                    Button(
                                            action: {
                                                isSummaryPresented.toggle()
                                            },
                                            label: {
                                                Text("Chart")
                                                        .padding(.vertical, 10)
                                                        .foregroundColor(.primary)
                                                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                                                        .overlay(
                                                                RoundedRectangle(cornerRadius: 8, style: .continuous)
                                                                        .stroke(Color(.dividerBg), lineWidth: onePx)
                                                        )
                                                        // Exactly here, otherwise re-rendering every second because of
                                                        // TabTimerView_ProgressView. This leads to twitch when scrolling.
                                                        .sheetEnv(isPresented: $isSummaryPresented) {
                                                            VStack {

                                                                ChartView()
                                                                        .padding(.top, 15)

                                                                Button(
                                                                        action: { isSummaryPresented.toggle() },
                                                                        label: { Text("close").fontWeight(.light) }
                                                                )
                                                                        .padding(.bottom, 4)
                                                            }
                                                        }
                                            }
                                    )

                                    Button(
                                            action: {
                                                isHistoryPresented.toggle()
                                            },
                                            label: {
                                                Text("History")
                                                        .padding(.vertical, 10)
                                                        .foregroundColor(.primary)
                                                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                                                        .overlay(
                                                                RoundedRectangle(cornerRadius: 8, style: .continuous)
                                                                        .stroke(Color(.dividerBg), lineWidth: onePx)
                                                        )
                                                        // Exactly here, otherwise re-rendering every second because of
                                                        // TabTimerView_ProgressView. This leads to twitch when scrolling.
                                                        .sheetEnv(isPresented: $isHistoryPresented) {
                                                            ZStack {
                                                                Color(.myBackground).edgesIgnoringSafeArea(.all)
                                                                HistoryView(isHistoryPresented: $isHistoryPresented)
                                                            }
                                                                    // todo
                                                                    .interactiveDismissDisabled()
                                                        }
                                            }
                                    )
                                }
                                        .frame(width: .infinity)
                                        .padding(.top, 24)
                                        .padding(.horizontal, 24)
                                        .listRowBackground(Color(.clear))

                                VStack {

                                    HStack(spacing: 16) {

                                        MenuTextButton(text: state.newActivityText) {
                                            isAddActivityPresented.toggle()
                                        }

                                        MenuTextButton(text: state.sortActivitiesText) {
                                            isEditActivitiesPresented.toggle()
                                        }

                                        MenuTextButton(text: state.settingsText) {
                                            isSettingsSheetPresented.toggle()
                                        }

                                        Spacer()
                                    }
                                            .padding(.bottom, 20)

                                    Text("Set a timer for each task to stay focused.")
                                            .fontWeight(.bold)
                                            .frame(maxWidth: .infinity, alignment: .leading)
                                            .padding(.bottom, 8)

                                    Text("No \"stop\" option is the main feature of this app. Once you have completed one activity, you have to set a timer for the next one, even if it's a \"sleeping\" activity.")
                                            .frame(maxWidth: .infinity, alignment: .leading)
                                            .padding(.bottom, 8)

                                    // todo
                                    //                                Text("This time-tracking approach provides real 24/7 data on how long everything takes. You can see it on the ")
                                    //                                        + Text("Chart").fontWeight(.bold)
                                    //                                        + Text(".")
                                    Text("This time-tracking approach provides real 24/7 data on how long everything takes. You can see it on the Chart.")
                                            .frame(maxWidth: .infinity, alignment: .leading)
                                }
                                        .frame(width: .infinity)
                                        .listRowBackground(Color(.clear))
                                        .foregroundColor(.secondary)
                                        .font(.system(size: 15))
                                        .lineSpacing(4)
                                        .myMultilineText()
                                        .padding(.top, 26)
                                        .padding(.horizontal, 26)

                                ZStack {
                                }
                                        .frame(height: 20)
                            }
                        }
                    }
                }
                        .sheetEnv(
                                isPresented: $isAddActivityPresented
                        ) {
                            ActivityFormSheet(
                                    isPresented: $isAddActivityPresented,
                                    editedActivity: nil
                            ) {
                            }
                        }
                        .sheetEnv(
                                isPresented: $isEditActivitiesPresented
                        ) {
                            EditActivitiesDialog(
                                    isPresented: $isEditActivitiesPresented
                            )
                        }
                        .sheetEnv(
                                isPresented: $isSettingsSheetPresented
                        ) {
                            SettingsSheet(isPresented: $isSettingsSheetPresented)
                        }
                        .sheetEnv(isPresented: $isReadmePresented) {
                            TabReadmeView(isPresented: $isReadmePresented)
                        }
                        .navigationBarHidden(true)
            }
        }
    }
}

private struct MenuTextButton: View {

    let text: String
    let onClick: () -> Void

    var body: some View {
        Button(
                action: {
                    onClick()
                },
                label: {
                    Text(text)
                            .foregroundColor(.blue)
                            .padding(.vertical, 4)
                }
        )
    }
}

struct TabTimerView_ActivityRowView: View {

    var activityUI: TabTimerVM.ActivityUI
    var lastInterval: IntervalModel
    var withTopDivider: Bool

    @State private var isSetTimerPresented = false
    @State private var isEditSheetPresented = false

    @State private var isActiveAnim = false

    var body: some View {
        MyListSwipeToActionItem(
                deletionHint: activityUI.deletionHint,
                deletionConfirmationNote: activityUI.deletionConfirmation,
                onEdit: {
                    isEditSheetPresented = true
                },
                onDelete: {
                    activityUI.delete()
                }
        ) {
            ZStack(alignment: .top) {
                AnyView(safeView)
                if withTopDivider {
                    DividerBg(xOffset: emojiStartPadding)
                }
            }
                    .padding(.horizontal, 21)
                    // todo remove after removing MyListSwipeToActionItem()
                    .background(Color(.bg))
        }
                .animateVmValue(value: activityUI.data.timerData != nil, state: $isActiveAnim)
    }

    private var safeView: some View {

        Button(
                action: {
                    isSetTimerPresented.toggle()
                },
                label: {

                    let endPadding = 12.0

                    VStack(alignment: .leading) {

                        HStack {

                            Text(activityUI.activity.emoji)
                                    .frame(width: emojiWidth)
                                    .padding(.horizontal, emojiHPadding)
                                    .font(.system(size: 26))

                            VStack {

                                Text(activityUI.data.listText)
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                        .foregroundColor(isActiveAnim ? .white : Color(.label))
                                        .truncationMode(.tail)
                                        .lineLimit(1)

                                if let listNote = activityUI.data.listNote {
                                    Text(listNote)
                                            .frame(maxWidth: .infinity, alignment: .leading)
                                            .padding(.top, onePx)
                                            .padding(.bottom, onePx)
                                            .foregroundColor(.white)
                                            .font(.system(size: 14, weight: .light))
                                }
                            }

                            Spacer()

                            ForEach(activityUI.timerHints, id: \.self) { hintUI in
                                Button(
                                        action: {
                                            hintUI.startInterval {}
                                        },
                                        label: {
                                            Text(hintUI.text)
                                                    .offset(y: onePx)
                                                    .font(.system(size: 15, weight: .light))
                                                    .foregroundColor(isActiveAnim ? .white : .blue)
                                                    .padding(.leading, 4)
                                                    .padding(.trailing, 4)
                                        }
                                )
                            }
                        }
                                .padding(.trailing, endPadding - 2)

                        TextFeaturesTriggersView(
                                triggers: activityUI.data.triggers,
                                paddingTop: 8.0,
                                paddingBottom: 4.0,
                                contentPaddingStart: emojiStartPadding - 1,
                                contentPaddingEnd: endPadding
                        )
                    }
                            .padding(.vertical, 11)
                            /// #TruncationDynamic + README_APP.md
                            .id("\(activityUI.activity.id) \(lastInterval.note)")
                }
        )
                .sheetEnv(isPresented: $isSetTimerPresented) {
                    ActivityTimerSheet(
                            activity: activityUI.activity,
                            isPresented: $isSetTimerPresented,
                            timerContext: nil,
                            onStart: {
                                isSetTimerPresented.toggle()
                                /// With animation twitching emoji
                            }
                    )
                            .background(Color(.mySecondaryBackground))
                            .presentationDetentsHeightIf16(ActivityTimerSheet.RECOMMENDED_HEIGHT, withDragIndicator: true)
                }
                .sheetEnv(isPresented: $isEditSheetPresented) {
                    ActivityFormSheet(
                            isPresented: $isEditSheetPresented,
                            editedActivity: activityUI.activity
                    ) {
                    }
                }
                .buttonStyle(ActivityButtonStyle(isActive: isActiveAnim))
                .clipShape(activityItemShape)
    }
}

///
/// Custom cell's implementation because the listRowBackground() hide touch effect
///
/// Dirty magic! While using inside halfSheet the buttons
/// don't work, .buttonStyle(.borderless) on halfSheet helps.
///
struct ActivityButtonStyle: ButtonStyle {

    let isActive: Bool

    func makeBody(configuration: Self.Configuration) -> some View {
        let bgColor = isActive ? Color.blue : Color(.bg)
        return configuration
                .label
                .background(configuration.isPressed ? Color(.systemGray4) : bgColor)
    }
}
