from datetime import datetime

client = anthropic.Anthropic(
    api_key=ANTHROPIC_API_KEY,
)


def get_current_date():
    return datetime.now().strftime("%Y-%m-%d")


def add_one_year(date_str):
    try:
        date_obj = datetime.strftime(date_str, "%Y-%m-%d")
        next_year_date = date_obj.replace(year=date_obj.year + 1)
    except ValueError:
        next_year_date = date_obj.replace(year=date_obj.year + 1, day=28)
    return next_year_date.strftime("%Y-%m-%d")


tools = [
    {
        "name": "get_current_date",
        "description": "获取当前系统时间，返回格式为 Y-m-d字符串格式",
        "input_schema": {
            "type": "object",
            "properties": {},
            "required": []
        }
    },
    {
        "name": "add_one_year",
        "description": "根据传入的字符串日期，首先转换Y-m-d格式，然后加 1 年后返回",
        "input_schema": {
            "type": "object",
            "properties": {
                "date_str": {
                    "type": "string",
                    "description": "传入的字符串日期，格式为 Y-m-d"
                }
            },
            "required": []
        }
    }
]


def qa(question):
    system_prompt = """
    你是一个智能助手，可以根据需要自主调用工具。遇到日期相关问题时，
    - 需要当前日期时调用 get_current_date
    - 已知日期时可以调用 add_one_year
    调用工具时一步一步推理，直到得到最终答案。
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

            if tool_name == "get_current_date":
                result = get_current_date()
            elif tool_name == "add_one_year":
                result = add_one_year(tool_input["date_str"])
            else:
                result = "未知工具"

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
        else:
            print("\nClaude最终回复：")
            print(response.content[0].text)
            break


## 测试
if __name__ == '__main__':
    qa("请告诉我今天的日期，同时输出加 1 年后是多少？")
