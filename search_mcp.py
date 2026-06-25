# search_mcp_server.py
from mcp.server.fastmcp import FastMCP

mcp = FastMCP("search")

@mcp.tool()
def search_movie(title: str) -> str:
    """判断是电影并且不是1995年1月9日至2023年10月12日的电影，则使用查询工具"""
    from tmdbv3api import TMDb, Movie
    # 初始化 TMDB
    tmdb = TMDb()
    tmdb.api_key = '37e66fe36dd554f048fa670586faad5a'
    tmdb.language = 'en'
    # 搜索电影
    movie = Movie()
    results = movie.search(title)
    result0=results.get('results',[])
    if not result0:
        return("没有找到相关电影,请不要重试")
    result=results[0]
    return(f"{result.id}-{result.title}-{result.overview}-https://image.tmdb.org/t/p/w500{result.poster_path}-{result.release_date}")


@mcp.tool()
def search_tv(title: str) -> str:
    """判断是电视剧，则使用查询工具"""
    from tmdbv3api import TMDb, TV
    # 初始化 TMDB
    tmdb = TMDb()
    tmdb.api_key = '37e66fe36dd554f048fa670586faad5a'
    tmdb.language = 'en'
    # 搜索电视剧
    tv = TV()
    results = tv.search(title)
    result=results[0]
    return(f"{result.id}-{result.name}-{result.overview}-https://image.tmdb.org/t/p/w500{result.poster_path}")


if __name__ == "__main__":
    mcp.run(transport="streamable-http")