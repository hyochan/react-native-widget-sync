//  widget.swift
//  widget
//

import WidgetKit
import SwiftUI
import Foundation

struct Provider: TimelineProvider {
    func placeholder(in context: Context) -> SimpleEntry {
        SimpleEntry(date: Date(), text: "Placeholder")
    }
    
    func getSnapshot(in context: Context, completion: @escaping (SimpleEntry) -> ()) {
        let entry = SimpleEntry(date: Date(), text: "Snapshot")
        completion(entry)
    }
    
    func getTimeline(in context: Context, completion: @escaping (Timeline<Entry>) -> ()) {
        let text = getItem()
        let entry = SimpleEntry(date: Date(), text: text)
        let timeline = Timeline(entries: [entry], policy: .never)
        completion(timeline)
    }

    private func getItem() -> String {
        let userDefaults = UserDefaults(suiteName: "group.com.example.widget")
        return userDefaults?.string(forKey: "todos") ?? ""
    }
    
}

struct SimpleEntry: TimelineEntry {
    let date: Date
    let text: String
}

struct TodoItem: Codable, Identifiable {
    let id: String
    let content: String
    let color: String
}

struct widgetEntryView : View {
    var entry: Provider.Entry

    func parseJsonString(_ jsonString: String) -> [TodoItem] {
        guard let data = jsonString.data(using: .utf8) else {
            print("Error: Could not convert string to data")
            return []
        }
        let decoder = JSONDecoder()
        do {
            let decodedItems = try decoder.decode([TodoItem].self, from: data)
            return decodedItems
        } catch {
            print("Decoding error: \(error)")
            return []
        }
    }

    func colorFromHexString(_ hex: String) -> Color {
        var hexSanitized = hex.trimmingCharacters(in: .whitespacesAndNewlines)
        hexSanitized = hexSanitized.replacingOccurrences(of: "#", with: "")
        
        var rgb: UInt64 = 0
        Scanner(string: hexSanitized).scanHexInt64(&rgb)
        
        let red = Double((rgb & 0xFF0000) >> 16) / 255.0
        let green = Double((rgb & 0x00FF00) >> 8) / 255.0
        let blue = Double(rgb & 0x0000FF) / 255.0
        
        return Color(red: red, green: green, blue: blue)
    }

    var body: some View {
        GeometryReader { geometry in
            let items: [TodoItem] = {
                let parsedItems = parseJsonString(entry.text)
                return parsedItems
            }()
          
            let userDefaults = UserDefaults(suiteName: "group.com.example.widget")

            VStack(spacing: 8) {
                if items.isEmpty {
                  VStack(alignment: .leading, spacing: 12) {
                      Spacer()
                      HStack {
                          ZStack {
                              Rectangle()
                                  .fill(colorFromHexString("AA33EE"))
                                  .frame(width: geometry.size.width * 0.15, height: geometry.size.width * 0.15)
                                  .cornerRadius(4)

                              Image(systemName: "checkmark")
                                  .font(.system(size: 18))
                                  .foregroundColor(Color.white)
                          }

                          Text(userDefaults?.string(forKey: "empty-string") ?? "All task completed!")
                              .padding(8)
                              .font(.system(size: 16, weight: .medium, design: .rounded))
                              .frame(width: geometry.size.width * 0.8, alignment: .leading)
                          Spacer()
                      }
                      Spacer()
                      Spacer()
                  }
                } else { // 아이템이 있을 경우
                    ForEach(items) { item in
                        HStack {
                            ZStack {
                                Rectangle()
                                    .fill(colorFromHexString(item.color))
                                    .frame(width: geometry.size.width * 0.15, height: geometry.size.width * 0.15)
                                    .cornerRadius(4)
                                
                                RoundedRectangle(cornerRadius: 2)
                                    .stroke(Color.white, lineWidth: 1)
                                    .frame(width: geometry.size.width * 0.075, height: geometry.size.width * 0.075)
                            }
                            Text(item.content)
                                .padding(8)
                                .font(.system(size: 16, weight: .medium, design: .rounded))
                                .frame(width: geometry.size.width * 0.8, alignment: .leading)
                            Spacer()
                        }
                    }
                }
            }
            .cornerRadius(12)
        }
    }
}

@main
struct widget: Widget {
    let kind: String = "widget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: Provider()) { entry in
            widgetEntryView(entry: entry)
        }
        .configurationDisplayName("dooboo")
        .description("The dooboo Todo Widget. Instantly view your tasks at a glance.")
        .supportedFamilies([.systemSmall])
    }
}

struct widget_Previews: PreviewProvider {
    static var previews: some View {
        widgetEntryView(entry: SimpleEntry(date: Date(), text: "[{\"id\": \"1111\", \"content\": \"5 minutes stretching\", \"color\": \"#FF3333\"}, {\"id\": \"2222\", \"content\": \"Voice Call\", \"color\": \"#AA33EE\"}]"))
            .previewContext(WidgetPreviewContext(family: .systemSmall))
    }
}
