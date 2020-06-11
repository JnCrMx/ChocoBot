package de.jcm.discord.chocobot.api;

public class ApiUser
{
	private long userId;
	private boolean operator;

	public ApiUser(long userId, boolean operator)
	{
		this.userId = userId;
		this.operator = operator;
	}

	public long getUserId()
	{
		return userId;
	}

	public void setUserId(long userId)
	{
		this.userId = userId;
	}

	public boolean isOperator()
	{
		return operator;
	}

	public void setOperator(boolean operator)
	{
		this.operator = operator;
	}
}
