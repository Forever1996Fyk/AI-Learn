
def get_weather(city:str) -> str:
    base_url = "https://api.openweathermap.org/data/2.5/weather?"
    # 添加 units=metric, 确保温度单位是摄氏度
    complete_url=f"{base_url}q={city}&appid={OPEN_WEATHER_API_KEY}&units=metric"

    try:
        response = requests.get(complete_url)
        # 抛出 HTTP 异常
        response.raise_for_status()
        data = response.json()

        if (data.get("cod")) != 404:
            main = data["main"]
            weather = data["weather"][0]
            wind = data["wind"]

            # 提取天气主要信息
            weather_description = weather.get("description")
            # 当前温度
            temperature = main.get("temp")
            # 体感温度
            feels_like = main.get("feels_like")
            # 最低温度
            temp_min = main.get("temp_min")
            # 最高温度
            temp_max = main.get("temp_max")
            # 气压
            pressure = main.get("pressure")
            # 湿度
            humidity = main.get("humidity")
            # 海平面气压
            sea_level = main.get("sea_Level")
            # 地面气压
            grnd_Level = main.get("grnd_Level")
            # 风速
            wind_speed = wind.get("speed")
            # 生成天气报告
            weather_report = (
                f"城市:{data.get('name')}\n"
                f"天气描述:{weather_description.capitalize()}\n"
                f"当前温度:{temperature}C\n"
                f"体感温度:{feels_like}C\n"
                f"最低温度:{temp_min}C\n"
                f"最高温度:{temp_max}.C\n"
                f"气压:{pressure} hPa\n"
                f"湿度:{humidity}%\n"
                f"海平面气压:{sea_level} hPa\n"
                f"地面气压:{grnd_Level} hPa\n"
                f"风速:{wind_speed} m/s\n"
            )
            return weather_report
        else:
            return "City Not Found!"
    except requests.exceptions.HTTPError as http_err:
        return f"HTTP error occurred: {http_err}"
    except Exception as err:
        return f"An error occurred: {err}"

get_weather_tool = {
    "name":"get_weather",
    "description":"使用 OpenWeatherMap API查询指定城市的天气情况，返回天气信息报告",
    "input_schema":{
        "type":"object",
        "properties":{
            "city":{
                "type":"string",
                "description":"城市名称，比如 London或 Beijing"
            }
        },
        "required":["city"]
    }
}

client = anthropic.Anthropic(
    api_key=ANTHROPIC_API_KEY,
)

def qa(question):
    system_prompt = """
    你是一个智能天气查询助手，可以通过 get_weather查询某城市的天气，并以中文进行回复
    """
    messages = [{"role": "user", "content": question}]
    while True:
        response = client.messages.create(
            model="claude-4.5-haiku",
            system=system_prompt,
            messages=messages,
            max_tokens=400,
            ## 告诉 claude 必要时调用工具
            tools=tools
        )

        print(f"\nClaude 返回: {response}")
        messages.append({"role": "assistant", "content": response.content})

        if response.stop_reson == 'tool_use':
            # 获取 Claude 提供的工具名称和输入
            tool_use = response.content[-1]
            tool_name = tool_use.name
            tool_input = tool_use.input
            tool_use_id = tool_use.id

            print(f"Claude 调用工具：{tool_name}, 输入参数:{tool_input}")

            if tool_name == "get_weather":
                try:
                    result = get_weather(tool_input["city"])
                    tool_result_message = {
                        "role": "user",
                        "content": [
                            {
                                "type": "tool_result",
                                "tool_use_id": tool_use_id,
                                "content": f"{result}"
                            }
                        ]
                    }
                    messages.append(tool_result_message)

                    final_response = client.messages.create(
                        model="claude-4.5-haiku",
                        system=system_prompt,
                        messages=messages,
                        max_tokens=400,
                    )
                    print("\nClaude最终回复：")
                    print(response.content[0].text)
                except ValueError as e:
                    print(f"Invalid input: {e}")
            else:
                print("Claude未使用工具，直接回复: ")
                print(response.content[0].text)



## 测试
if __name__ == '__main__':
    qa("上海天气如何")